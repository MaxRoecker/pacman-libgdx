package com.theosirian.pacman;

import com.badlogic.gdx.ai.pfa.Connection;
import com.badlogic.gdx.ai.pfa.indexed.IndexedNode;
import com.badlogic.gdx.utils.Array;

/**
 * <p>
 * </p>
 * <p>Created at 2015-09-10 19-59.</p>
 */
public class PathNode implements IndexedNode {

    private int index;
    private Array<Connection> connections;


    @Override
    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    @Override
    public Array<Connection> getConnections() {
        return this.connections;
    }

    public void setConnections(Array<Connection> connections) {
        this.connections = connections;
    }
}
