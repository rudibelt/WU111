package com.example.wu111;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

final class ConnectionManager {
    private static Set<WeakReference<ConnectionEventListener>> listeners = new HashSet<WeakReference<ConnectionEventListener>>();


    public ConnectionManager(Set<WeakReference<ConnectionEventListener>> listeners) {
        this.listeners = listeners;
    };

    public static void  registerListener(ConnectionEventListener listener) {
        //if (listeners.map { it.get() }.contains(listener)) { return }
        listeners.add(new WeakReference(listener));
        //listeners = listeners.filter { it.get() != null }.toMutableSet()
        //Log.d("Added listener $listener, ${listeners.size} listeners total");
    }

    public static void  invoke(boolean connected)
    {
        if (connected) {
            listeners.forEach(connectionEventListenerWeakReference -> {
                ConnectionEventListener c = connectionEventListenerWeakReference.get();
                if (c != null) { c.onConnectionSetupComplete();}
            });
        }
        else
        {
            listeners.forEach(connectionEventListenerWeakReference -> {
                ConnectionEventListener c = connectionEventListenerWeakReference.get();
                if (c != null) { c.onDisconnect();}
            });
        }
    }
}
