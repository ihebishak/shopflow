package com.shopflow.catalog.infrastructure.mongo.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.shopflow.catalog.application.api.AddVariantUseCase;
import com.shopflow.catalog.application.api.command.AddVariantCommand;
import com.shopflow.catalog.application.service.AddVariantService;
import com.shopflow.catalog.application.service.GetSellerProductService;
import com.shopflow.catalog.application.spi.ProductEventPublisher;
import com.shopflow.catalog.application.spi.ProductRepository;
import com.shopflow.catalog.domain.Product;
import com.shopflow.catalog.domain.ProductModifiedConcurrentlyException;
import com.shopflow.catalog.domain.SellerId;
import com.shopflow.catalog.domain.Variant;
import com.shopflow.catalog.infrastructure.mongo.mapper.ProductDocumentMapperImpl;
import com.shopflow.kernel.money.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.testcontainers.containers.MongoDBContainer;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Deux ajouts simultanés sur le même produit, avec de vraies transactions MongoDB. Les verrous fixent l'ordre des
 * opérations : sans eux, le test dépendrait de la vitesse de chaque fil d'exécution.
 */
@SpringJUnitConfig(AddVariantTransactionTest.TestConfiguration.class)
class AddVariantTransactionTest {

    private static final SellerId KARIM = new SellerId("seller-karim");
    private static final Money PRICE = Money.euros(16_000);
    private static final String PHONE_SIZE = "44";
    private static final int TIMEOUT_SECONDS = 20;

    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:7.0");

    /** Démarré ici : le contexte Spring a besoin de son adresse dès sa création. */
    static {
        MONGO.start();
    }

    @Autowired
    private AddVariantUseCase addVariantUseCase;

    @MockitoSpyBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductEventPublisher productEventPublisher;

    @Test
    @DisplayName("SHOULD refuse the second add and keep the first variant WHEN the product changed after being read")
    void shouldRefuseSecondAddWhenProductChangedAfterBeingRead() throws InterruptedException {
        // GIVEN
        Product airMax = savedAirMax();
        CountDownLatch phoneHasRead = new CountDownLatch(1);
        CountDownLatch computerHasCommitted = new CountDownLatch(1);
        doAnswer(invocation -> {
            phoneHasRead.countDown();
            computerHasCommitted.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return invocation.callRealMethod();
        }).when(productRepository).save(argThat(AddVariantTransactionTest::isAddedByThePhone));

        // WHEN
        CompletableFuture<Variant> phone = CompletableFuture.supplyAsync(() -> addVariantFromThePhone(airMax));
        assertThat(phoneHasRead.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        addVariantUseCase.addVariant(commandFor(airMax, "43"));
        computerHasCommitted.countDown();
        Throwable error = catchThrowable(phone::join);

        // THEN
        assertThat(error).hasCauseInstanceOf(ProductModifiedConcurrentlyException.class);
        assertThat(sizesOf(airMax)).containsExactly("42", "43");
        assertOnlyEventIsFor(airMax, "43");
    }

    @Test
    @DisplayName("SHOULD refuse the second add and keep the first variant WHEN the other transaction is still open")
    void shouldRefuseSecondAddWhenOtherTransactionIsStillOpen() throws InterruptedException {
        // GIVEN
        Product airMax = savedAirMax();
        CountDownLatch phoneHasSaved = new CountDownLatch(1);
        CountDownLatch computerHasFinished = new CountDownLatch(1);
        doAnswer(invocation -> {
            Object result = invocation.callRealMethod();
            phoneHasSaved.countDown();
            computerHasFinished.await(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            return result;
        }).when(productRepository).save(argThat(AddVariantTransactionTest::isAddedByThePhone));

        // WHEN
        CompletableFuture<Variant> phone = CompletableFuture.supplyAsync(() -> addVariantFromThePhone(airMax));
        assertThat(phoneHasSaved.await(TIMEOUT_SECONDS, TimeUnit.SECONDS)).isTrue();
        Throwable error = catchThrowable(() -> addVariantUseCase.addVariant(commandFor(airMax, "43")));
        computerHasFinished.countDown();
        phone.join();

        // THEN
        assertThat(error).isInstanceOf(ProductModifiedConcurrentlyException.class);
        assertThat(sizesOf(airMax)).containsExactly("42", PHONE_SIZE);
        assertOnlyEventIsFor(airMax, PHONE_SIZE);
    }

    private void assertOnlyEventIsFor(final Product product, final String size) {
        verify(productEventPublisher).publishProductVariantAdded(eq(product.getId()),
                argThat(variant -> size.equals(variant.getSize())));
        verifyNoMoreInteractions(productEventPublisher);
    }

    private Product savedAirMax() {
        Product airMax = Product.draft(KARIM, "Air Max 90 Infrared", "Nike", "sneakers", null, Instant.now())
                .addVariant(Variant.create("42", "Rouge", PRICE));
        productRepository.save(airMax);
        return airMax;
    }

    private Variant addVariantFromThePhone(final Product product) {
        return addVariantUseCase.addVariant(commandFor(product, PHONE_SIZE));
    }

    private static AddVariantCommand commandFor(final Product product, final String size) {
        return AddVariantCommand.builder().sellerId(KARIM).productId(product.getId()).size(size).color("Rouge")
                .price(PRICE).build();
    }

    private static boolean isAddedByThePhone(final Product product) {
        return product != null && product.getVariants().stream().anyMatch(v -> PHONE_SIZE.equals(v.getSize()));
    }

    private List<String> sizesOf(final Product product) {
        return productRepository.findById(product.getId()).orElseThrow().getVariants().stream()
                .map(Variant::getSize).toList();
    }

    @Configuration
    @EnableTransactionManagement
    @Import({AddVariantService.class, GetSellerProductService.class, MongoProductRepository.class,
            ProductDocumentMapperImpl.class})
    @ImportAutoConfiguration(ValidationAutoConfiguration.class)
    static class TestConfiguration {

        @Bean(destroyMethod = "close")
        MongoClient mongoClient() {
            return MongoClients.create(MONGO.getReplicaSetUrl());
        }

        @Bean
        MongoDatabaseFactory mongoDatabaseFactory(final MongoClient mongoClient) {
            return new SimpleMongoClientDatabaseFactory(mongoClient, "catalog-test");
        }

        @Bean
        MongoTemplate mongoTemplate(final MongoDatabaseFactory mongoDatabaseFactory) {
            return new MongoTemplate(mongoDatabaseFactory);
        }

        @Bean
        MongoTransactionManager transactionManager(final MongoDatabaseFactory mongoDatabaseFactory) {
            return new MongoTransactionManager(mongoDatabaseFactory);
        }
    }
}
