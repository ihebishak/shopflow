package com.shopflow.testsupport.architecture.fixture.good.application.api.command;

public class RenameThingCommand {

    private final String name;

    public RenameThingCommand(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
