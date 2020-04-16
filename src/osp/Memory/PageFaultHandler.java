package osp.Memory;

import java.util.*;
import osp.Hardware.*;
import osp.Threads.*;
import osp.Tasks.*;
import osp.FileSys.FileSys;
import osp.FileSys.OpenFile;
import osp.IFLModules.*;
import osp.Interrupts.*;
import osp.Utilities.*;
import osp.IFLModules.*;

/**
 * The page fault handler is responsible for handling a page fault. If a swap in
 * or swap out operation is required, the page fault handler must request the
 * operation.
 * 
 * @OSPProject Memory
 */
public class PageFaultHandler extends IflPageFaultHandler {
	/**
	 * This method handles a page fault.
	 * 
	 * It must check and return if the page is valid,
	 * 
	 * It must check if the page is already being brought in by some other thread,
	 * i.e., if the page has already pagefaulted (for instance, using
	 * getValidatingThread()). If that is the case, the thread must be suspended on
	 * that page.
	 * 
	 * If none of the above is true, a new frame must be chosen and reserved until
	 * the swap in of the requested page into this frame is complete.
	 * 
	 * Note that you have to make sure that the validating thread of a page is set
	 * correctly. To this end, you must set the page's validating thread using
	 * setValidatingThread() when a pagefault happens and you must set it back to
	 * null when the pagefault is over.
	 * 
	 * If no free frame could be found, then a page replacement algorithm must be
	 * used to select a victim page to be replaced.
	 * 
	 * If a swap-out is necessary (because the chosen frame is dirty), the victim
	 * page must be dissasociated from the frame and marked invalid. After the
	 * swap-in, the frame must be marked clean. The swap-ins and swap-outs must be
	 * preformed using regular calls to read() and write().
	 * 
	 * The student implementation should define additional methods, e.g, a method to
	 * search for an available frame, and a method to select a victim page making
	 * its frame available.
	 * 
	 * Note: multiple threads might be waiting for completion of the page fault. The
	 * thread that initiated the pagefault would be waiting on the IORBs that are
	 * tasked to bring the page in (and to free the frame during the swapout).
	 * However, while pagefault is in progress, other threads might request the same
	 * page. Those threads won't cause another pagefault, of course, but they would
	 * enqueue themselves on the page (a page is also an Event!), waiting for the
	 * completion of the original pagefault. It is thus important to call
	 * notifyThreads() on the page at the end -- regardless of whether the pagefault
	 * succeeded in bringing the page in or not.
	 * 
	 * @param thread        the thread that requested a page fault
	 * @param referenceType whether it is memory read or write
	 * @param page          the memory page
	 * 
	 * @return SUCCESS is everything is fine; FAILURE if the thread dies while
	 *         waiting for swap in or swap out or if the page is already in memory
	 *         and no page fault was necessary (well, this shouldn't happen,
	 *         but...). In addition, if there is no frame that can be allocated to
	 *         satisfy the page fault, then it should return NotEnoughMemory
	 * 
	 * @OSPProject Memory
	 */
	public static int do_handlePageFault(ThreadCB thread, int referenceType, PageTableEntry page) {
		// your code goes here
		if (page.isValid()) {
			return FAILURE;
		}
		FrameTableEntry NFrame = null;
		NFrame = getFreeFrame();

		
		if (NFrame == null) {
			NFrame = SecondChance();
			if (NFrame == null)
				return NotEnoughMemory;
		}

	page.setValidatingThread(thread);
 	Event event = new SystemEvent("PageFaultHappend");
	thread.suspend(event);
		

		if (!NFrame.isReserved() && NFrame.getLockCount() <= 0 ) {

			//event = new SystemEvent("PageFaultHappend");
			//thread.suspend(event);
			//page.setValidatingThread(thread);
			NFrame.setReserved(thread.getTask());
			//page.setFrame(NFrame);
			//page.getTask().getSwapFile().read(page.getID(), page, thread);
			
		}

		PageTableEntry Npage = NFrame.getPage();
		if (Npage != null) {

			if (NFrame.isDirty()) {

				NFrame.getPage().getTask().getSwapFile().write(NFrame.getPage().getID(), NFrame.getPage(), thread);

				if (thread.getStatus() == GlobalVariables.ThreadKill) {
					page.notifyThreads();
					event.notifyThreads();
					ThreadCB.dispatch();
					return FAILURE;

				}

				NFrame.setDirty(false);

			}

			/*
			 * NFrame.setReferenced(false); NFrame.setPage(null); Npage.setValid(false);
			 * Npage.setFrame(null);
			 */

			NFrame.setReferenced(false);
			if (Npage != null && Npage.getFrame().getLockCount() == 0) {
				NFrame.setPage(null);
				Npage.setValid(false);
				Npage.setFrame(null);

			}

		}

		page.setFrame(NFrame);
		//NFrame.setPage(page);
		page.getTask().getSwapFile().read(page.getID(), page, thread);

		if (thread.getStatus() == ThreadKill) {



			page.setValidatingThread(null);
			page.setFrame(null);
			page.notifyThreads();

			if (NFrame.getReserved() == thread.getTask()) {
				NFrame.setUnreserved(thread.getTask());
			}

			NFrame.setReferenced(false);
			NFrame.setDirty(false);
			event.notifyThreads();
			NFrame.setPage(null);
			ThreadCB.dispatch();
			return FAILURE;
		}


		page.setValid(true);
		if (NFrame.getReserved() == thread.getTask())
		{
			NFrame.setUnreserved(thread.getTask());
		}
		NFrame.setReferenced(true);
		page.notifyThreads();
		event.notifyThreads();

		if (referenceType == MemoryWrite) {
			NFrame.setDirty(true);
		} else {
			NFrame.setDirty(false);
		}

		page.setValidatingThread(null);
	//	page.notifyThreads();
	//	event.notifyThreads();
		ThreadCB.dispatch();
		return SUCCESS;
	}


	static int numFreeFrames() {

		int curentFreeFrames = 0;
		FrameTableEntry frame;
		for (int i = 0; i < MMU.getFrameTableSize(); i++) {
			frame = MMU.getFrame(i);
			if ((frame.getPage() == null) && (!frame.isReserved()) && (frame.getLockCount() == 0) && frame != null) {
				curentFreeFrames++;
			}
		}
		return curentFreeFrames;

	}

	// Looks for a free frame; returns the first free frame starting the search from frame[0].
	static FrameTableEntry getFreeFrame() {
		//int curentFreeFrames = 0;
		FrameTableEntry frame = null;
		int i =0;
		while (i < MMU.getFrameTableSize()) {
			frame = MMU.getFrame(i);
			if ((!frame.isReserved() && frame.getLockCount() == 0)) {
				break;
			}
			i++;
		}
//		b: for (int i = 0; i < MMU.getFrameTableSize(); i++) {
//			frame = MMU.getFrame(i);
//			if ((!frame.isReserved()) || (frame.getLockCount() == 0)) {
//				//curentFreeFrames++;
//				break b;
//			}
//
//		}
		return frame;

	}

	 static FrameTableEntry SecondChance() {
		FrameTableEntry frame;
		boolean isdirty = true;
		int frameID = 0;
		int counter = 0;
		
		//Phase I - Batch freeing of occupied frames the clean. 
		
		while (counter > (2 * MMU.getFrameTableSize())) {
			if(numFreeFrames() >= MMU.wantFree) {
			
				frame = MMU.getFrame(MMU.Cursor);

				//1. If a page's reference bit is set, clear it and move to the next frame 
			if (frame.isReferenced()) {
				frame.setReferenced(false);
				MMU.Cursor++; // or could try frame = MMU.getFrame(MMU.Cursor + 1);
			}

			// 2. Finding a clean frame
			if (frame.isReferenced() == false && frame.isReserved() == false && frame.isDirty() == false
					&& frame.getLockCount() == 0) {
				// a. freeing the frame
				frame.setPage(null);
				frame.setDirty(false);
				frame.setReferenced(false);
				// b. Updating a page table
				frame.getPage().setFrame(null);
				frame.getPage().setValid(false);
				
				
			}
			if (frame.isDirty() && isdirty && frame.getLockCount() == 0 && frame.isReserved() == false) {
				frameID = frame.getID();
				isdirty = false;
			}
			MMU.Cursor = (MMU.Cursor + 1) % MMU.getFrameTableSize();
			counter++;
		}
		}
		/*
		 * if (numFreeFrames() != MMU.wantFree && !isdirty) { return frameID; } if
		 * (numFreeFrames() < MMU.wantFree && isdirty) { FrameTableEntry freeFrame =
		 * getFreeFrame(); return freeFrame; }
		 */
		
		/*- Phase II - Skip if the number of free frames is wantFree, otherwise do the following: */
		
		if (numFreeFrames() != MMU.wantFree) {
			if (!isdirty)
				return new FrameTableEntry(frameID);

			if (numFreeFrames() < MMU.wantFree) {
				FrameTableEntry freeFrame = getFreeFrame();
				return freeFrame;
			}

		}
		/* Phase III - Phase one managed to free "wantFree" frames */
		else {
			if (numFreeFrames() == MMU.wantFree) {
				FrameTableEntry freeFrame = getFreeFrame();
				return freeFrame;
			}
		}

		return null;

	}

	/*
	 * Feel free to add methods/fields to improve the readability of your code
	 */

}

/*
 * Feel free to add local classes to improve the readability of your code
 */