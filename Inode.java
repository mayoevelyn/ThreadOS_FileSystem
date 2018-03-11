public class Inode {
	private final static int iNodeSize = 32;       // fix to 32 bytes
	private final static int directSize = 11;      // # direct pointers

	public int length;                             // file size in bytes
	public short count;                            // # file-table entries pointing to this
	public short flag;                             // 0 = unused, 1 = used, ...
	public short direct[] = new short[directSize]; // direct pointers
	public short indirect;                         // a indirect pointer

	Inode( ) {                                     // a default constructor
		length = 0;
		count = 0;
		flag = 1;
		for ( int i = 0; i < directSize; i++ )
			direct[i] = -1;
		indirect = -1;
	}

	//helper so you can use ints in other classes
	Inode(int iNumber) {
		this((short)iNumber);
	}

	//Loads iNode from disk
	Inode( short iNumber ) {
		// prep the inode block
		int blockNumber = 1 + iNumber / 16; // 1 for superblock
		byte[] data = new byte[Disk.blockSize];
		int offset = (iNumber % 16) * 32;

		// read the data
		SysLib.rawread(blockNumber, data);

		// read the values appropriately
		length = SysLib.bytes2int(data, offset);
		offset += 4;
		count = SysLib.bytes2short(data, offset);
		offset += 2;
		flag = SysLib.bytes2short(data, offset);
		offset += 2;

		// read 11 direct blocks
		for(int i = 0; i < 11; ++i) {
			direct[i] = SysLib.bytes2short(data, offset);
		}
		// read indirect blocks
		indirect = SysLib.bytes2short(data, offset);
	}

	//save to disk as the given node
	void toDisk( short iNumber ) {
		
		//create the array we'll save
		byte[] asArray = new byte[iNodeSize];
		int blockNumber = 1 + iNumber / 16;
		int offset = 0;
		
		//start writing values into our array
		SysLib.int2bytes(length, asArray, offset);
		offset += 4;
		SysLib.short2bytes(count, asArray, offset);
		offset += 2;
		SysLib.short2bytes(flag, asArray, offset);
		offset += 2;
		
		//write the direct array
		for (int i = 0; j < 11; i++) {
			SysLib.short2bytes(direct[i], asArray, offset);
			offset += 2;
		}
		
		//write the indirect
		SysLib.short2bytes(indirect, asArray, offset);
		
		//read in what's currently there
		byte[] data = new byte[Disk.blockSize];
		SysLib.rawread( blockNumber, data );
		
		//copy our new byte representation in
		System.arraycopy(asArray, 0, data, iNumber % 16 * 32, 32);
		
		//write to disk
		SysLib.rawwrite(blockNumber, data);
	}
	
	//helper method
	int toDisk(int iNumber) {
		return toDisk((short) iNumber);
	}

	//Finds the block that a seek pointer at this offset would be referencing
	int getBlock(int offset) {
		// check that offset is valid
		if(offset < 0)
			return -1;

		// 512 bytes in a block
		int blockNumber = offset / 512;
		// if its a direct block, scan the inode block
		if (blockNumber < 11) {
			return direct[blockNumber];
		}
		// if not direct, check if indirect is used
		// if not, it doesn't exist
		else if (indirect < 0) {
			return -1;
		}
		// check indirect block
		else {
			byte[] block = new byte[512];
			SysLib.rawread(indirect, block);
			int var4 = blockNumber - 11;
			return SysLib.bytes2short(block, var4 * 2);
		}
	}

	// error cases:
	// -1: direct block already taken

	int setBlock(int offset, short iNumber) {
		// check that offset is valid
		if(offset < 0)
			return -1;

		// find offset in block
		int blockOffset = offset / 512;
		// if blockOffset points to a direct block
		if (blockOffset < 11) {
			// direct block already taken
			if (direct[blockOffset] >= 0) {
				return -1;
			}
			//
			else if (blockOffset > 0 && direct[blockOffset - 1] == -1) {
				return -2;
			}
			// success
			else {
				// set the direct block
				direct[blockOffset] = iNumber;
				return 0;
			}
		} else if (this.indirect < 0) {
			return -3;
		} else {
			byte[] var4 = new byte[512];
			SysLib.rawread(this.indirect, var4);
			int var5 = var3 - 11;
			if (SysLib.bytes2short(var4, var5 * 2) > 0) {
				SysLib.cerr("indexBlock, indirectNumber = " + var5 + " contents = " + SysLib.bytes2short(var4, var5 * 2) + "\n");
				return -1;
			} else {
				SysLib.short2bytes(iNumber, var4, var5 * 2);
				SysLib.rawwrite(this.indirect, var4);
				return 0;
			}
		}
	}
	
	//Returns the block number that contains the indirect block for this iNode
	short getIndirectBlock() {
		return indirect;
	}

	//Sets the location of the indirect block
	boolean setIndirectBlock( short indirectBlockNumber ) {
		// check that every direct block is used up first
		for(int i = 0; i < 11; ++i) {
			if (direct[i] == -1)
				return false;
		}

		// if indirect block is already allocated, return false
		if (indirect != -1) {
			return false;
		} else {
			indirect = indirectBlockNumber;
			byte[] newIndirectBlock = new byte[512];

			// reserve half for data
			for(int i = 0; i < 256; ++i) {
				// offset * 2 because short is 2 bytes
				SysLib.short2bytes((short)-1, newIndirectBlock, i*2);
			}

			SysLib.rawwrite(indirectBlockNumber, newIndirectBlock);
			return true;
		}
	}

	// tell inode block to not use indirect block anymore
	byte[] removeIndirectBlock(){
		// check that indirect pointer still valid
		if (indirect >= 0) {
			// temp block holding indirect block
			byte[] indirectBlock = new byte[512];
			// read the indirect block from disk
			SysLib.rawread(indirect, indirectBlock);
			// set indirect block pointer as not used
			indirect = -1;
			// return the removed indirect block
			return indirectBlock;
		}
		// indirect block not used anyway
		else {
			return null;
		}
	}
}
