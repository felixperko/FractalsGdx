package de.felixp.fractalsgdx.client;

public abstract class SharedDataListener implements ISharedDataListener {

    String identifier;

    public SharedDataListener(String identifier) {
        this.identifier = identifier;
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

}
