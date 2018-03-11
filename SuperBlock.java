public class SuperBlock {
	private final int defaultInodeBlocks = 64;
	public int totalBlocks; // the number of disk blocks
	public int totalInodes; // the number of inode blocks
	public int freeList;    // the block number of the free list's head

	public SuperBlock( int diskSize ) {
		// read the superblock from disk
		byte[] superBlock = new byte[Disk.blockSize];
		SysLib.rawread( 0, superBlock );
		totalBlocks = SysLib.bytes2int( superBlock, 0 );
		totalInodes = SysLib.bytes2int( superBlock, 4 );
		freeList = SysLib.bytes2int( superBlock, 8 );

		if ( totalBlocks == diskSize && totalInodes > 0 && freeList >= 2 )
			// disk contents are valid
			return;
		else {
			// need to format disk
			totalBlocks = diskSize;
			format( defaultInodeBlocks );
		}
	}

	// wrapper
	void format() {
		format(defaultInodeBlocks);
	}

	void format( int inodeSize ) {
		// 4 blocks for inodes if 64 inodes
		totalInodes = inodeSize;

		for(short i = 0; i < totalInodes; ++i) {
			Inode newInode = new Inode();
			newInode.flag = 0;
			newInode.toDisk(i);
		}
		// 2 for super block + first inode reserved by directory
		freeList = 2 + ((totalInodes*32)/512);

		for(int i = freeList; i < totalBlocks; ++i) {
			byte[] newBlock = new byte[512];

			for(int j = 0; j < 512; ++j) {
				newBlock[j] = 0;
			}

			SysLib.int2bytes(i + 1, newBlock, 0);
			SysLib.rawwrite(i, newBlock);
		}

		sync();
	}

	// Write back totalBlocks, inodeBlocks, and freeList to disk
	void sync() {
		byte[] block = new byte[512];
		SysLib.int2bytes(totalBlocks, block, 0);
		SysLib.int2bytes(totalInodes, block, 4);
		SysLib.int2bytes(freeList, block, 8);
		SysLib.rawwrite(0, block);
		SysLib.cerr("Superblock synchronized\n");
	}

	// Dequeue the top block from the free list
	public int getFreeBlock() {
		// old free block head
		int freeHead = freeList;
		// check that freelist head block is valid
		if (freeHead != -1) {
			// allocate new empty block
			byte[] emptyBlock = new byte[512];
			// write old pointer to the new block
			SysLib.rawread(freeHead, emptyBlock);

			// update new free list with ptr to empty block
			freeList = SysLib.bytes2int(emptyBlock, 0);
			// write new pointer to disk
			SysLib.int2bytes(0, emptyBlock, 0);
			SysLib.rawwrite(freeHead, emptyBlock);
		}
		// return the free block number
		return freeHead;
	}

	// Enqueue a given block to the end of the free list
	public boolean returnBlock ( int blockNumber ) {
		// check for valid block number
		if (blockNumber < 0) {
			return false;
		} else {
			byte[] emptyBlock = new byte[512];

			// empty block by setting each byte as 0
			for(int i = 0; i < 512; ++i) {
				emptyBlock[i] = 0;
			}

			// get block number in bytes to write to disk
			SysLib.int2bytes(freeList, emptyBlock, 0);
			SysLib.rawwrite(blockNumber, emptyBlock);
			// set the new empty block to
			freeList = blockNumber;
			return true;
		}
	}
}
