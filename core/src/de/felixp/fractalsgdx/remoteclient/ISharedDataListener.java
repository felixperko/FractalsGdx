package de.felixp.fractalsgdx.remoteclient;

import de.felixperko.fractals.data.shareddata.DataContainer;

interface ISharedDataListener {

    void receivedDataContainer(DataContainer dataContainer);
    String getIdentifier();
}
