/* 
 * Copyright 2010 by AVM GmbH <info@avm.de>
 *
 * This software contains free software; you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License ("License") as 
 * published by the Free Software Foundation  (version 3 of the License). 
 * This software is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the copy of the 
 * License you received along with this software for more details.
 */

package de.usbi.android.util.adapter;

import java.util.ArrayList;
import java.util.Collection;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

/**
 * Einfache Erweiterung des BaseAdapters, in Richtung eines ArrayAdapters,
 * welche zur Vermeidung von Code-Dublizierung die Implementierung der Methoden
 * getCount(), getItem() und getItemId() erspart.
 * 
 * @param <T>
 *            Typ welchen die Elemente im Array des Adapters haben.
 */
public abstract class ArrayAdapterExt<T> extends BaseAdapter {

	private final ArrayList<T> entries = new ArrayList<T>();

	public ArrayAdapterExt() {
		// Nichts zu tun.
	}

	public ArrayAdapterExt(Collection<T> entries) {
		addEntries(entries);
	}

	protected final void clearEntries() {
		this.entries.clear();
	}

	protected final void addEntry(T entry) {
		this.entries.add(entry);
	}

	protected final void addEntries(Collection<T> entries) {
		this.entries.addAll(entries);
	}

	@Override
	public final int getCount() {
		return this.entries.size();
	}

	@Override
	public final T getItem(int position) {
		return this.entries.get(position);
	}

	@Override
	public final long getItemId(int position) {
		return position;
	}

	@Override
	public final View getView(int position, View view, ViewGroup viewGroup) {
		T item = getItem(position);
		return populateView(item, view, viewGroup);
	}

	/**
	 * Befülle die einzelnen Views mit den Daten aus dem Array.
	 * 
	 * @param item
	 *            mit den Daten vom Typ des ArrayAdapters
	 * @param view
	 *            der mit den Daten befüllt werden soll. Falls der view != null
	 *            ist kann man ihn wiederverwendet, wenn nicht muss der View neu
	 *            inflated werden.
	 * @param viewGroup
	 */
	public abstract View populateView(final T item, View view,
			ViewGroup viewGroup);
}