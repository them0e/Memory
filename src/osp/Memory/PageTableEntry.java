package osp.Memory;

import osp.Hardware.*;
import osp.Tasks.*;
import osp.Threads.*;
import osp.Devices.*;
import osp.Utilities.*;
import osp.IFLModules.*;
/**
   The PageTableEntry object contains information about a specific virtual
   page in memory, including the page frame in which it resides.

   @OSPProject Memory
*/

public class PageTableEntry extends IflPageTableEntry
{
    /**
       The constructor. Must call

       	   super(ownerPageTable,pageNumber);

       as its first statement.

       @OSPProject Memory
    */
    public PageTableEntry(PageTable ownerPageTable, int pageNumber)
    {
    	// calling super
    			super(ownerPageTable, pageNumber);

    }



    /** 
    Purpose: Increasing the lock count on the page by one.
		   - FIRST increment lockCount, 
		   - THEN check if the page is valid, and if it is not and no
		     page validation event is present for the page, start page fault
		     by calling PageFaultHandler.handlePageFault(). 

	 Return: SUCCESS or FAILURE
	   FAILURE happens when the pagefault due to locking fails or the
	   that created the IORB thread gets killed.
    			  
   @OSPProject Memory
   
   Authors: Abdulaziz Hasan 1555528, Mohammed Shukri 1647376
   Date of the Last modification: 15/4/2020
*/
    public int do_lock(IORB iorb)
    {
    	// Getting the I/O request block thread on the page. 
    	ThreadCB iorbThread = iorb.getThread();
    			// Check if the page isn't valid
    			if (!isValid()) {
    				// To help identify the pages that are involved in a pagefault.
    				// Check if the validation thread is not present.
    				if (getValidatingThread() == null) {
    					// Start a pagefault process.
    					PageFaultHandler.handlePageFault(iorbThread, GlobalVariables.MemoryLock, this);
    					// Check if the pagefault fails, return FAILURE in that case. 
    					if (iorbThread.getStatus() == ThreadKill) {
    						return FAILURE;
    					}
    				}

    				// If the validating thread is present, check if the thread caused
    				// the pagefault is equal to this thread.
    				else if (getValidatingThread() != iorbThread) {

    					// Suspend thread
    					iorbThread.suspend(this);
//					When the page becomes valid (or if the pagefault handler fails to make the
//					page valid, say, because the original thread, that caused the pagefault
//					was killed during the wait), the threads waiting on the page will be un-
//					blocked by the pagefault handler and will be able to continue. 
//    				When such threads become unblocked inside the do lock() method
// 					control falls through the call to suspend() and the do lock() method must exit
//    				and return the appropriate value: SUCCESS if the page became valid as a result of the pagefault
//    				and FAILURE otherwise.
    					if(!isValid()) {

    						return FAILURE;
    					}
    				}

    			}

    	    	// increment lockCount
    			getFrame().incrementLockCount();
    			return SUCCESS;

    }


    /** 
    Purpose: Decreasing the lock count on the page by one.
	   		 This method will decrement lockCount, but not below zero.
	   		 
   @OSPProject Memory
   
   Authors: Abdulaziz Hasan 1555528, Mohammed Shukri 1647376
   Date of the Last modification: 15/4/2020
*/
    public void do_unlock()
    {
		// Decrementing lockCount if is not equal or less than 0 
		if (getFrame().getLockCount() > 0) {getFrame().decrementLockCount();}
    }


}
