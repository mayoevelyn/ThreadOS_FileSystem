import java.util.*;

public class FileSystem
{
	private SuperBlock superblock;
	private Directory directory;
	private FileTable filetable;

	private static short SEEK_SET = 0;
	private static short SEEK_CUR = 1;
	private static short SEEK_END = 2;

	public FileSystem (int diskBlocks)
	{
		superblock = new SuperBlock(diskBlocks);
		directory = new Directory(superblock.totalInodes);
		filetable = new FileTable (directory);
		
		//read the '/' file from the disk
		FileTableEntry entryDirectory = open("/", "r");
		int directorySize = fsize(entryDirectory);
		if (directorySize > 0) //the directory has some data
		{
			byte[] directoryData = new byte[directorySize];
			read (entryDirectory, directoryData);
			directory.bytes2directory(directoryData);
		}
		close(entryDirectory);
	}

	public int format(int files)
	{
	    if (files > 0)
	    {
            superblock.format(files);
            directory = new Directory(superblock.totalInodes);
            filetable = new FileTable(directory);
            return 0;
        }
        else
        {
            return -1;
        }
	}

	public void sync()
    {
        byte[] oldDirectory  = directory.directory2bytes();
        FileTableEntry rootFile = open("/", "w");
        write(rootFile, directory.directory2bytes());
        close(rootFile);
        superblock.sync();
    }
    public int seek(FileTableEntry target, int offset, int whence)
    {
        int targetLocation = target.seekPtr;
        if (whence == SEEK_SET)
        {
            target.seekPtr = offset;
        }
        else if (whence == SEEK_CUR)
        {
            target.seekPtr += offset;
        }
        else if (whence == SEEK_END)
        {
            target.seekPtr = target.inode.length + offset;
        }
        else
        {
            return -1;
        }

        if (target.seekPtr > target.inode.length)
        {
            target.seekPtr = target.inode.length;
        }

        if (target.seekPtr < 0)
        {
            target.seekPtr = 0;
        }

        return target.seekPtr;
    }

    public FileTableEntry open(String fileName, String mode)
	{
	    FileTableEntry fileToOpen = filetable.falloc(fileName, mode);

	    if (mode == "w")
        {
            if (!deallocateBlocks(fileToOpen))
            {
                return null;
            }
        }

        return fileToOpen;
	}

	public synchronized int write(FileTableEntry target, byte buffer[])
    {
        if (target == null || target.mode == "r")
        {
            return -1;
        }
        int totalBytesWritten = 0;
        int bufferLength = buffer.length;
        int blockLength = 512;

        while (bufferLength > 0) {
            int currBlock = target.inode.getBlock(target.seekPtr);
            if (currBlock == -1) {
                short newBlock = (short) superblock.getFreeBlock();
                switch (target.inode.setBlock(target.seekPtr, newBlock)) {
                    case -3:
                        short blockTest = (short) this.superblock.getFreeBlock();

                        if (target.inode.setIndirectBlock(blockTest) == false) {
                            return -1;
                        }

                        if (target.inode.setBlock(target.seekPtr, newBlock) != 0) {
                            return -1;
                        }
                    case 0:
                    default:
                        currBlock = newBlock;
                        break;
                    case -1:
                    case -2:
                    case -4:
                        return -1;
                }
            }

            byte[] shadowBuffer = new byte[blockLength];
            SysLib.rawread(currBlock, shadowBuffer);

            int writeLoc = target.seekPtr % blockLength;
            int writingLength = blockLength - writeLoc;

            if (writingLength < bufferLength)
            {
                System.arraycopy(buffer, totalBytesWritten, shadowBuffer, writeLoc, writingLength);
                SysLib.rawwrite(currBlock, shadowBuffer);
                target.seekPtr += writingLength;
                totalBytesWritten += writingLength;
                bufferLength -= writingLength;
            }
            else
            {
                System.arraycopy(buffer, totalBytesWritten, shadowBuffer, writeLoc, bufferLength);
                SysLib.rawwrite(currBlock, shadowBuffer);
                target.seekPtr += bufferLength;
                totalBytesWritten += bufferLength;
                bufferLength = 0;
            }

            if (target.inode.length <= target.seekPtr) {
                target.inode.length = target.seekPtr;
            }
        }
        target.inode.toDisk(target.iNumber);
        return totalBytesWritten;
    }

    public synchronized int read(FileTableEntry target, byte buffer[])
    {
        int totalBytesRead = 0;
        int blockLength = Disk.blockSize;
        int bufferLength = buffer.length;
        if ((target.mode != "w") && (target.mode != "a"))
        {

            int bufferSize = 0;
            while (bufferLength > 0 && target.seekPtr < this.fsize(target))
            {
                int currBlock = target.inode.getBlock(target.seekPtr);

                if (currBlock == -1)
                {
                    break;
                }
                byte[] shadowBuffer = new byte[blockLength];
                SysLib.rawread(currBlock, shadowBuffer);

                int readLoc = target.seekPtr % blockLength;
                int readingLength = blockLength - readLoc;
                int remaining = this.fsize(target) - target.seekPtr;
                int smallestRead = Math.min(Math.min(readingLength, bufferLength), remaining);

                System.arraycopy(shadowBuffer, readLoc, buffer, totalBytesRead, smallestRead);
                target.seekPtr += smallestRead;
                totalBytesRead += smallestRead;
                bufferLength -= smallestRead;
            }
            return totalBytesRead;
        }
        else
        {

            return -1;
        }
    }

	public synchronized int close(FileTableEntry target)
	{
	    if (target == null)
        {
            return -1;
        }

        if(target.count > 0)
        {
            target.count--;
        }

	    if (target.count == 0)
        {
            target.inode.toDisk(target.iNumber);
            if (filetable.ffree(target))
            {
                return 0;
            }
            else
            {
                return -1;
            }
        }
        return 0;
	}
	
	public synchronized int delete(String fileName)
	{
	    int inodeNumber = directory.namei(fileName);

	    if (inodeNumber == -1)
        {
            return -1;
        }

        Inode tempNode = new Inode(inodeNumber);

	    if (!directory.ifree((short)inodeNumber))
        {
            return -1;
        }

        tempNode.count = 0;
	    tempNode.toDisk(inodeNumber);
	    return 0;
	}
	
	public int fsize(FileTableEntry target)
	{
	    return target.inode.length;
	}

	private boolean deallocateBlocks(FileTableEntry target)
    {
        Vector<Integer> releasedBlocks = target.inode.deallocAllBlocks(target.iNumber);
        for (int i = 0; i < releasedBlocks.size(); i++)
        {
            superblock.returnBlock(releasedBlocks.elementAt(i));
        }
        return true;
    }
}
