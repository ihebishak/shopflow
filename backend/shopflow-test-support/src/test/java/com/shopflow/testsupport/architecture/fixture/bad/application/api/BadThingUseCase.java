package com.shopflow.testsupport.architecture.fixture.bad.application.api;

import com.shopflow.testsupport.architecture.fixture.bad.application.api.command.RenameBadThingCommand;

/** Oublie volontairement {@code @Valid} sur sa commande. */
public interface BadThingUseCase {

    void renameThing(RenameBadThingCommand command);
}
