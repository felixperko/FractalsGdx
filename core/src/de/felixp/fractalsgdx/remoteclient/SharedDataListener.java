package de.felixp.fractalsgdx.remoteclient;

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
