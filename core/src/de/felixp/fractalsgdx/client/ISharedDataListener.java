package de.felixp.fractalsgdx.client;

import de.felixperko.fractals.data.shareddata.DataContainer;

interface ISharedDataListener {

    void receivedDataContainer(DataContainer dataContainer);
    String getIdentifier();
}
