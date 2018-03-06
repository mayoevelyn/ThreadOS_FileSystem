import java.util.*;

public class FileySystem
{
	private SuperBlock superblock;
	private Directory directoy;
	private FileTable filetable;

	public FileSystem (int diskBlocks)
	{
		superblock = new SuperBlock(diskBlocks);
		directory = new Directory(superblock.toralInodes);
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
		superblock.format(files);
		directory = new Directory(superblock.inodeBlocks);
		filetable = new FileTable(directory);
		return 0;
	}
	
	public int open(String fileName, String mode)
	{
	}

	public int read(int fd, byte buffer[])
	{
	}

	public int seek(int fd, int offset, int whence)
	{
	}

	public int close(int fd)
	{
	}
	
	public int delete(String fileName)
	{
	}
	
	public int fsize(int fd)
	{
	}
}
