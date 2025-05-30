/*
 * @(#)EventListenerList.java	1.38 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.jaudiotagger.utils.tree;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Array;
import java.util.EventListener;

/**
 * A class that holds a list of EventListeners.  A single instance
 * can be used to hold all listeners (of all types) for the instance
 * using the list.  It is the responsiblity of the class using the
 * EventListenerList to provide type-safe API (preferably conforming
 * to the JavaBeans spec) and methods which dispatch event notification
 * methods to appropriate Event Listeners on the list.
 * 
 * The main benefits that this class provides are that it is relatively
 * cheap in the case of no listeners, and it provides serialization for 
 * event-listener lists in a single place, as well as a degree of MT safety
 * (when used correctly).
 *
 * Usage example:
 *    Say one is defining a class that sends out FooEvents, and one wants
 * to allow users of the class to register FooListeners and receive 
 * notification when FooEvents occur.  The following should be added
 * to the class definition:
 * <pre>
 * EventListenerList listenerList = new EventListenerList();
 * FooEvent fooEvent = null;
 *
 * public void addFooListener(FooListener l) {
 *     listenerList.add(FooListener.class, l);
 * }
 *
 * public void removeFooListener(FooListener l) {
 *     listenerList.remove(FooListener.class, l);
 * }
 *
 *
 * // Notify all listeners that have registered interest for
 * // notification on this event type.  The event instance 
 * // is lazily created using the parameters passed into 
 * // the fire method.
 *
 * protected void fireFooXXX() {
 *     // Guaranteed to return a non-null array
 *     Object[] listeners = listenerList.getListenerList();
 *     // Process the listeners last to first, notifying
 *     // those that are interested in this event
 *     for (int i = listeners.length-2; i>=0; i-=2) {
 *         if (listeners[i]==FooListener.class) {
 *             // Lazily create the event:
 *             if (fooEvent == null)
 *                 fooEvent = new FooEvent(this);
 *             ((FooListener)listeners[i+1]).fooXXX(fooEvent);
 *         }
 *     }
 * }
 * </pre>
 * foo should be changed to the appropriate name, and fireFooXxx to the
 * appropriate method name.  One fire method should exist for each
 * notification method in the FooListener interface.
 * <p>
 * <strong>Warning:</strong>
 * Serialized objects of this class will not be compatible with
 * future Swing releases. The current serialization support is
 * appropriate for short term storage or RMI between applications running
 * the same version of Swing.  As of 1.4, support for long term storage
 * of all JavaBeans<sup><font size="-2">TM</font></sup>
 * has been added to the <code>java.beans</code> package.
 * Please see {@link java.beans.XMLEncoder}.
 *
 * @version 1.38 03/23/10
 * @author Georges Saab
 * @author Hans Muller
 * @author James Gosling
 */
public class EventListenerList implements Serializable {
    /* A null array to be shared by all empty listener lists*/
    private final static Object[] NULL_ARRAY = new Object[0];
    /* The list of ListenerType - Listener pairs */
    protected transient Object[] listenerList = NULL_ARRAY;

    /**
     * Passes back the event listener list as an array
     * of ListenerType-listener pairs.  Note that for 
     * performance reasons, this implementation passes back 
     * the actual data structure in which the listener data
     * is stored internally!  
     * This method is guaranteed to pass back a non-null
     * array, so that no null-checking is required in 
     * fire methods.  A zero-length array of Object should
     * be returned if there are currently no listeners.
     * 
     * WARNING!!! Absolutely NO modification of
     * the data contained in this array should be made -- if
     * any such manipulation is necessary, it should be done
     * on a copy of the array returned rather than the array 
     * itself.
     */
    public Object[] getListenerList() {
	return listenerList;
    }

    /**
     * Return an array of all the listeners of the given type. 
     * @return all of the listeners of the specified type. 
     * @exception  ClassCastException if the supplied class
     *		is not assignable to EventListener
     * 
     * @since 1.3
     */
    public <T extends EventListener> T[] getListeners(Class<T> t) {
	Object[] lList = listenerList; 
	int n = getListenerCount(lList, t); 
        T[] result = (T[])Array.newInstance(t, n); 
	int j = 0; 
	for (int i = lList.length-2; i>=0; i-=2) {
	    if (lList[i] == t) {
		result[j++] = (T)lList[i+1];
	    }
	}
	return result;   
    }

    /**
     * Returns the total number of listeners for this listener list.
     */
    public int getListenerCount() {
	return listenerList.length/2;
    }

    /**
     * Returns the total number of listeners of the supplied type 
     * for this listener list.
     */
    public int getListenerCount(Class<?> t) {
	Object[] lList = listenerList;
        return getListenerCount(lList, t);
    }

    private int getListenerCount(Object[] list, Class t) {
        int count = 0;
	for (int i = 0; i < list.length; i+=2) {
	    if (t == list[i])
		count++;
	}
	return count;
    }

    /**
     * Adds the listener as a listener of the specified type.
     * @param t the type of the listener to be added
     * @param l the listener to be added
     */
    public synchronized <T extends EventListener> void add(Class<T> t, T l) {
	if (l==null) {
	    // In an ideal world, we would do an assertion here
	    // to help developers know they are probably doing
	    // something wrong
	    return;
	}
	if (!t.isInstance(l)) {
	    throw new IllegalArgumentException("Listener " + l +
					 " is not of type " + t);
	}
	if (listenerList == NULL_ARRAY) {
	    // if this is the first listener added, 
	    // initialize the lists
	    listenerList = new Object[] { t, l };
	} else {
	    // Otherwise copy the array and add the new listener
	    int i = listenerList.length;
	    Object[] tmp = new Object[i+2];
	    System.arraycopy(listenerList, 0, tmp, 0, i);

	    tmp[i] = t;
	    tmp[i+1] = l;

	    listenerList = tmp;
	}
    }

    /**
     * Removes the listener as a listener of the specified type.
     * @param t the type of the listener to be removed
     * @param l the listener to be removed
     */
    public synchronized <T extends EventListener> void remove(Class<T> t, T l) {
	if (l ==null) {
	    // In an ideal world, we would do an assertion here
	    // to help developers know they are probably doing
	    // something wrong
	    return;
	}
	if (!t.isInstance(l)) {
	    throw new IllegalArgumentException("Listener " + l +
					 " is not of type " + t);
	}
	// Is l on the list?
	int index = -1;
	for (int i = listenerList.length-2; i>=0; i-=2) {
	    if ((listenerList[i]==t) && (listenerList[i + 1].equals(l))) {
		index = i;
		break;
	    }
	}
	
	// If so,  remove it
	if (index != -1) {
	    Object[] tmp = new Object[listenerList.length-2];
	    // Copy the list up to index
	    System.arraycopy(listenerList, 0, tmp, 0, index);
	    // Copy from two past the index, up to
	    // the end of tmp (which is two elements
	    // shorter than the old list)
	    if (index < tmp.length)
		System.arraycopy(listenerList, index+2, tmp, index, 
				 tmp.length - index);
	    // set the listener array to the new array or null
	    listenerList = (tmp.length == 0) ? NULL_ARRAY : tmp;
	    }
    }

    // Serialization support.  
    private void writeObject(ObjectOutputStream s) throws IOException {
	Object[] lList = listenerList;
	s.defaultWriteObject();
	
	// Save the non-null event listeners:
	for (int i = 0; i < lList.length; i+=2) {
	    Class t = (Class)lList[i];
	    EventListener l = (EventListener)lList[i+1];
	    if ((l!=null) && (l instanceof Serializable)) {
		s.writeObject(t.getName());
		s.writeObject(l);
	    }
	}
	
	s.writeObject(null);
    }

    private void readObject(ObjectInputStream s) 
	throws IOException, ClassNotFoundException {
        listenerList = NULL_ARRAY;
	s.defaultReadObject();
	Object listenerTypeOrNull;
	
	while (null != (listenerTypeOrNull = s.readObject())) {
            ClassLoader cl = Thread.currentThread().getContextClassLoader();
	    EventListener l = (EventListener)s.readObject();
	    add((Class<EventListener>)Class.forName((String)listenerTypeOrNull, true, cl), l);
	}	    
    }

    /**
     * Returns a string representation of the EventListenerList.
     */
    public String toString() {
	Object[] lList = listenerList;
	String s = "EventListenerList: ";
	s += lList.length/2 + " listeners: ";
	for (int i = 0 ; i <= lList.length-2 ; i+=2) {
	    s += " type " + ((Class)lList[i]).getName();
	    s += " listener " + lList[i+1];
	}
	return s;
    }
}
