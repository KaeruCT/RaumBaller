package jgame.impl;

import java.util.Random;

/** Data structure that can be used as replacement for Hashtable&lt;String&gt;
 * for fast sorted enumeration of its elements.  It can get and put like a
 * Hashtable.  Get uses binary search, and is a little, but not much,  slower
 * than Hashtable.get.  Put and remove are expensive as they require shifting
 * the array (algorithm duration is linear wrt the array size).  It is
 * possible to put or remove multiple elements at a time (using a second
 * SortedArray), which is faster.

 * <P> Array enumeration is done by simply traversing the keys and/or values
 * array, which are filled with elements from 0 to size, exclusive.  This is
 * about 10 times faster than using the Hashtable.getElements/getKeys
 * enumerators, and the elements are sorted (in ascending order).

 * <P> SortedArray grows automatically to accommodate the required elements.
 * It starts with given initialcapacity and grows with increments
 * initialcapacity each time the capacity is exceeded.

 */
public class SortedArray {

	int capacity;
	int growspeed;

	public int size=0;
	public String [] keys;
	public Object [] values;

	public SortedArray(int initialcapacity) {
		capacity=initialcapacity;
		growspeed = initialcapacity;
		keys = new String [capacity];
		values = new Object [capacity];
	}

	public void clear() {
		clear(0);
	}

	void clear(int startidx) {
		for (int i=startidx; i<size; i++) {
			keys[i]=null;
			values[i]=null;
		}
		size=startidx; 
	}

	public void put(SortedArray elem) {
		// go through elem, and
		// (1) put the elements of which keys are already present
		// (2) store the indexes of the elements of which keys are not present
		int [] idxes = new int[elem.size];
		int oldsize=size;
		int newsize=size;
		for (int i=elem.size-1; i>=0; i--) {
			int idx = get(elem.keys[i]);
			//System.out.println("IDX:"+idx);
			if (idx >= 0) {
				keys[idx]=elem.keys[i];
				values[idx]=elem.values[i];
				// set size to last idx found, so that get() doesn't search the
				// last part
				size=idx;
			} else {
				size = -1-idx;
				newsize++;
			}
			idxes[i]=-1-idx;
		}
		// restore old size, so grow doesn't get confused
		size = oldsize;
		// now, shift the array to store the remaining elements
		if (newsize > capacity) grow(newsize-oldsize);
		int oldi=oldsize-1;
		int newi=newsize-1;
		for (int i=elem.size-1; i>=0; i--) {
			if (idxes[i]<0) continue;
			//System.out.println("idx:"+idxes[i]);
			while (oldi>=idxes[i]) {
				keys[newi]=keys[oldi];
				values[newi--]=values[oldi--];
			}
			keys[newi]=elem.keys[i];
			values[newi--]=elem.values[i];
		}
		size=newsize;
		//checkSanity();
	}

	public void put(String key,Object value) {
		int idx = get(key);
		if (idx>=0) {
			keys[idx] = key;
			values[idx] = value;
			return;
		}
		if (size+1 > capacity) grow(1);
		size++;
		idx = -1-idx;
		for (int i=size-1; i>idx; i--) {
			keys[i] = keys[i-1];
			values[i] = values[i-1];
		}
		keys[idx] = key;
		values[idx] = value;
		//checkSanity();
	}

	void grow(int amount) {
		capacity += amount+growspeed;
		String [] newkeys = new String[capacity];
		Object [] newvalues = new Object [capacity];
		for (int i=0; i<size; i++) {
			newkeys[i] = keys[i];
			newvalues[i] = values[i];
		}
		keys = newkeys;
		values = newvalues;
	}

	public void remove(String key) {
		int idx = get(key);
		if (idx>=0) {
			values[idx]=null;
			removeNullValues(idx);
		}
		//checkSanity();
	}

	public void remove(SortedArray elem) {
		int lowidx=size;
		int oldsize=size;
		for (int i=elem.size-1; i>=0; i--) {
			int idx = get(elem.keys[i]);
			if (idx>=0) {
				values[idx]=null;
				lowidx=idx;
				size=idx;
			} else {
				size=-1-idx;
			}
		}
		size=oldsize;
		//lowidx points to the lowest index of the nulled value indexes
		if (lowidx<size) removeNullValues(lowidx);
		//checkSanity();
	}

	void removeNullValues(int firstidx) {
		for (int i=firstidx; i<size; i++) {
			if (values[i]!=null) {
				keys[firstidx] = keys[i];
				values[firstidx++] = values[i];
			}
		}
		clear(firstidx);
	}

	public int get(String key) {
		/*
		// algorithm 2 (wikipedia)
		int low = 0;
		int high = size;
		while (low < high) {
			int mid = (low + high)/2;
			if (keys[mid].compareTo(key) < 0) { 
				low = mid + 1; 
			} else {
				high = mid; //can't be high = mid-1: here keys[mid] >= value,
							//  so high can't be < mid if keys[mid] == value
			}   
		}
		if (low < size && keys[low].equals(key))
			return low;
		else
			return -1-low;
		*/
		/*
		// algorithm 1 (wikipedia)
		int low = 0;
		int high = size-1;
		while (low <= high) {
			int mid = (low + high) / 2;
			int cmp = keys[mid].compareTo(key);
			if (cmp>0) {
				high = mid - 1;
			else if (cmp<0) {
				low = mid + 1;
			else
				return mid;
		}
		return -1;*/
		// algorithm 1 returning position to insert a new value
		int low = 0;
		int high = size-1;
		int cmp=0;
		while (low <= high) {
			int mid = (low + high) / 2;
			cmp = keys[mid].compareTo(key);
			if (cmp>0) {
				high = mid - 1;
			} else if (cmp<0) {
				low = mid + 1;
			} else {
				return mid;
			}
		}
		return -1-low;
   }

	public String toString() {
		String res="SortedArray";
		for (int i=0; i<size; i++) {
			res += "{"+keys[i]+"/"+values[i]+"},";
		}
		return res;
	}

	/** do some sanity checks */
	void checkSanity() {
		for (int i=0; i<size; i++) {
			if (keys[i] == null || values[i] == null) {
				System.err.println("####### Sanity check failed! ######");
				System.err.println(toString());
				throw new Error("Assertion failure!");
			}
		}
	}

	/** test method */
	public static void main(String [] args) {
		System.out.println("Testing SortedArray ...");
		Object dummyobject = "dummy";
		SortedArray arr_accum = new SortedArray(20);
		Random random = new Random();
		for (int n=0; n<1000; n++) {
			// create two random sorted arrays and combine them
			SortedArray arr1 = new SortedArray(21);
			SortedArray arr2 = new SortedArray(22);
			// second array has more collisions
			for (int i=0; i<100; i++) {
				arr2.put("key"+(int)(100+20*random.nextDouble()), dummyobject);
				arr2.remove("key"+(int)(100+20*random.nextDouble()));
			}
			// first array has few collisions
			for (int i=0; i<100; i++) {
				arr1.put("key"+(int)(100+100*random.nextDouble()), dummyobject);
				arr1.remove("key"+(int)(100+100*random.nextDouble()));
			}
			arr1.checkSanity();
			arr2.checkSanity();
			arr1.remove(arr2);
			arr1.checkSanity();
			SortedArray arr_added = new SortedArray(23);
			arr_added.put(arr2);
			arr_added.put(arr1);
			arr_added.checkSanity();
			arr_added.remove(arr2);
			arr_added.checkSanity();
			arr_added.remove(arr1);
			arr_added.checkSanity();
			arr_accum.put(arr1);
			arr_accum.remove(arr2);
			arr_accum.checkSanity();
			//System.out.println("Array 1:" + arr1+"\n");
			//System.out.println("Array 2:" + arr2+"\n");
			//System.out.println("Combined:" + arr_added+"\n");
		}
	}

}

