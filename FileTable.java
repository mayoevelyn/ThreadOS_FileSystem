import java.util.*;

public class FileTable
{
	private Vector<FileTableEntry> fileTable; //Caches references to a file 
	private Directory fileDirectory; //used to represent the structure of the files on DISK

	public static short IDLE = 0; //Represents a file that has not been used
	public static short USED = 1; //represents a file that has been used
	public static short READING = 2; //represents a file that is currently being READ
	public static short WRITING = 3; //represents a file that is in WRITE, READ/WRITE, or APPEND mode

	//CONSTRUCTOR
	public FileTable(Directory directory)
	{
		fileTable = new Vector<FileTableEntry>();
		fileDirectory = directory;
	}

	//Allocates a reference to a file into a Vector list
	//Purpose: Prevents multiple sources from writing to a file at the same time,
	//	   reading from a file as it is being written, or writing to a file that
	//	   is being read.
	//	   Additionaly, keeps track of the amount of references to a file
	//	   Also, if a file is being written to that does not exist it will
	//	   be created, while preventing a nonexistant file from being read
	public synchronized FileTableEntry falloc(String filename, String fileMode)
	{
		//temporary values
		short inodeNumber = -1;
		Inode inode = null;

		while (true)
		{
			//determines inodeNumber to be allocated to file table
			if (filename.equals("/"))
			{
				inodeNumber = 0; //root directory == inode "0"
			}
			else
			{
				//Retrieves the inode number from the given file
				//wheter valid(>=1) or invalid (<0)
				inodeNumber = fileDirectory.namei(filename);
			}

			if (inodeNumber >= 0) //existing inodes
			{
				inode = new Inode(inodeNumber);
				
				if (fileMode.equals("r")) //File is in READ mode
				{
					if(inode.flag == READING ||
					   inode.flag == USED ||
					   inode.flag == IDLE) 
					{
						//change flag to READ mode to
						//reflect file mode
						inode.flag = READING;
						break; //Breaks away from while loop 
					}
					else if (inode.flag == WRITING) 
					{
						//file is currently being written to
						try 
						{
							//Reading must wait for writing to
							//finish
							wait(); 
						}
						catch (InterruptedException error) {}
					}
				}
				else //FILE is in WRITE, READ/WRITE, or APPEND mode
				{
					if (inode.flag == USED || inode.flag == IDLE)
					{
						//No current reading/writing processes
						inode.flag = WRITING;
						break; //breaks away from while loop
					}
					else //file is currently being read or altered
					{
						try 
						{
							wait();
						}
						catch (InterruptedException Error) {}
					}
				}
			}
			else if (!fileMode.equals("r"))
			{
				//file hasnt been allocated to an inode and
				//mode is WRITE, READ/WRITE, or APPEND
				inodeNumber = fileDirectory.ialloc(filename);
				inode = new Inode(inodeNumber);
				inode.flag = WRITING;
				break; //breaks away from while loop
			}
			else //FILE hasnt been allocated and mode is in READ
			{
				return null; //NO valid process being performed
			}
		} //END OF WHILE LOOP	
		
		inode.count++; //increase the number of references to the FILE
		inode.toDisk(inodeNumber); 
			
		//create an entry that keeps track of reference to the file
		FileTableEntry newEntry = new FileTableEntry(inode, inodeNumber, fileMode);
		//adds the entry to the file table
		fileTable.addElement(newEntry);
		return newEntry;
	}

	//This function removes an active file reference when it completes its process 
	public synchronized boolean ffree(FileTableEntry targetEntry)
	{
		Inode inode = new Inode(targetEntry.iNumber);
		
		//Attempts removal of the reference from the table
		if (fileTable.remove(targetEntry)) 
		{
			//Notifies the next process waiting for the file
			if (inode.flag == READING) 
			{
				if (inode.count == 1)
				{
					//Multiple reads can happen simultatneously so 
					//all that should be waiting will be something
					//that alters the file itself
					notify(); 
					inode.flag = USED;
				}
			}
			else if (inode.flag == WRITING)
			{
				//Only one process can alter a file at a time so
				//there could be multiple reads that need to be 
				//notified to clear them from waiting.
				inode.flag = USED;
				notifyAll();
			}

			inode.count--; //decrements the amount of active references to a file
			inode.toDisk(targetEntry.iNumber); //save the status of the inode to disk
			return true; //removal of reference is successful
		}
		else
		{
			return false; //removal of reference failed
		}
		return false; //safety end of process
	}

	//Checks if the filetable is empty of all references to any files
	public synchronized boolean fempty()
	{
		//Returns true if table is empty; returns false if not empty
		return fileTable.isEmpty();
	}
}
