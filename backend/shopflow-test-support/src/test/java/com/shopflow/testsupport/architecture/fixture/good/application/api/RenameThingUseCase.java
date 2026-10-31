package com.shopflow.testsupport.architecture.fixture.good.application.api;

import com.shopflow.testsupport.architecture.fixture.good.application.api.command.RenameThingCommand;
import jakarta.validation.Valid;

public interface RenameThingUseCase {

    String renameThing(@Valid RenameThingCommand command);
}
