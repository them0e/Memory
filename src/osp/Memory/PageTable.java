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
    Purpose: This is the page table constructor. Will call

	       super(ownerTask)

	   as its first statement. Then it will figure out
	   the size of a page table and create the page table, 
	   populating it with items of type, PageTableEntry.

    			  
   @OSPProject Memory
   
   Authors: Abdulaziz Hasan 1555528, Mohammed Shukri 1647376
   Date of the Last modification: 15/4/2020
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
    Purpose: Freeing up main memory occupied by the task.
       Then unreserving the freed pages, if necessary.
   @OSPProject Memory
   
   Authors: Abdulaziz Hasan 1555528, Mohammed Shukri 1647376
   Date of the Last modification: 15/4/2020
*/
    public void do_deallocateMemory()
    {
    	TaskCB task = getTask();
    	
    	for (int i=0; i< MMU.getFrameTableSize(); i++) {
    		
    		
    		
    		FrameTableEntry frame = MMU.getFrame(i);
    		PageTableEntry page = frame.getPage();
    				

    		if ( page != null && page.getTask() == task)
    		{
    			// Nullify the page.
    			frame.setPage(null);
    	    	// Clean the page.
    			frame.setDirty(false);
    	    	// Unset the reference.
    			frame.setReferenced(false);
    			
    			// Check if the task reserved a given frame then unreserve the freed pages.
    			if(task == frame.getReserved())
    				frame.setUnreserved(task);
    		}
    		
    	}
    	

    }



}

