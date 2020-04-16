package osp.Memory;
/**
    The PageTable class represents the page table for a given task.
    A PageTable consists of an array of PageTableEntry objects.  This
    page table is of the non-inverted type.

    @OSPProject Memory
*/
import java.lang.Math;
import osp.Tasks.*;
import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Hardware.*;

public class PageTable extends IflPageTable
{
	int arraySize;
	/**
	   The page table constructor. Must call

	       super(ownerTask)

	   as its first statement. Then it must figure out
	   what should be the size of a page table, and then
	   create the page table, populating it with items of 
	   type, PageTableEntry.

	   @OSPProject Memory
    */
    public PageTable(TaskCB ownerTask)
    {
    	// call super
    	super(ownerTask);
    	// get the size of page table
    	int size = MMU.getPageAddressBits();
    	arraySize = (int) Math.pow(2, size);
    	// create page table array
    	pages = new PageTableEntry[arraySize];
    	
    	// initialize the pages
    	for (int i = 0; i< pages.length; i++)
    	pages[i] = new PageTableEntry(this, i);


    }

    /**
       Frees up main memory occupied by the task.
       Then unreserves the freed pages, if necessary.

       @OSPProject Memory
    */
    public void do_deallocateMemory()
    {
    	TaskCB task = getTask();
    	
    	for (int i=0; i< MMU.getFrameTableSize(); i++) {
    		
    		
    		
    		FrameTableEntry frame = MMU.getFrame(i);
    		PageTableEntry page = frame.getPage();
    				
    		//TaskCB frameTask = page.getTask();
    		if ( page != null && page.getTask() == task)
    		{
    			// nullify the page
    			frame.setPage(null);
    	    	// clean the page
    			frame.setDirty(false);
    	    	// unset the reference
    			frame.setReferenced(false);
    			
    			// check if the task reserved a given frame then unreserves the freed pages 
    			if(task == frame.getReserved())
    				frame.setUnreserved(task);
    		}
    		
    	}
    	

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
