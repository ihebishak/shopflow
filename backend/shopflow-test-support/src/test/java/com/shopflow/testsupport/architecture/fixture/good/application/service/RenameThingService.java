package com.shopflow.testsupport.architecture.fixture.good.application.service;

import com.shopflow.testsupport.architecture.fixture.good.application.api.RenameThingUseCase;
import com.shopflow.testsupport.architecture.fixture.good.application.api.command.RenameThingCommand;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;

@Service
@Validated
public class RenameThingService implements RenameThingUseCase {

    @Override
    public String renameThing(final RenameThingCommand command) {
        return command.getName();
    }
}
