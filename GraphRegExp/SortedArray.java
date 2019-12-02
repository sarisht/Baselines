
import java.util.*;


// Manage sorted arrays of Objects with integer keys.
public class SortedArray
{
    private ArrayList<Integer>keys;
    private ArrayList<Object>array;
    

    public SortedArray(int initialCapacity)
    {
        keys = new ArrayList<Integer>(initialCapacity);
        array = new ArrayList<Object>(initialCapacity);
    }
    public SortedArray()
    {
        this(16);
    }
    public SortedArray(int key,Object value)
    {
        this();
        keys.add(key);
        array.add(value);
    }
    
    
  
    
    public int GetKeyAt(int index)
    {
        return keys.get(index);
    }
    public Object GetAt(int index)
    {
        return array.get(index);
    }
    public Object Get(int key)
    {
        int res = Collections.binarySearch(keys,key);
        if (res < 0) return null;
        return array.get(res);
    }
    public int GetPos(int key)
    {
        return Collections.binarySearch(keys, key);
    }
    public boolean Contains(int key)
    {
        return (Collections.binarySearch(keys,key) >= 0);
    }
    
    public void Insert(int key,Object value)
    {
        int length = keys.size();
        if (length == 0)
        {
            keys.add(key);
            array.add(value);
            return;
        }
        if (length == 1)
        {
            if (key < keys.get(0))
            {
                keys.add(0,key);
                array.add(0,value);
            } else {
                keys.add(key);
                array.add(value);
            }
        } else {
            int insertPos = Collections.binarySearch(keys, key);
            if (insertPos < 0) insertPos = -insertPos - 1;
            keys.add(insertPos,key);
            array.add(insertPos,value);
        }
    }
    
    public int InsertIfNotInside(int key,Object value)
    {
        int pos = Collections.binarySearch(keys,key);
        if (pos < 0)
        {
            pos = -pos - 1;
            keys.add(pos,key);
            array.add(pos,value);
        }
        return pos;
    }
    public boolean InsertUnique(int key,Object value)
    {
        int pos = Collections.binarySearch(keys,key);
        if (pos < 0)
        {
            pos = -pos - 1;
            keys.add(pos,key);
            array.add(pos,value);
            return true;
        }
        return false;
    }
    
    public void Remove(int key)
    {
        int pos = Collections.binarySearch(keys, key);
        if (pos >= 0)
        {
            keys.remove(pos);
            array.remove(pos);
        }
    }
    
    
    public void SetDataAt(int index,Object data)
    {
        array.set(index,data);
    }
    
    
    public int Size()
    {
        return keys.size();
    }
}


