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
		totalInodes = SysLib.bytes2int( superblock, 4 );
		freeList = SysLib.bytes2int( superblock, 8 );

		if ( totalBlocks == diskSize && totalInodes > 0 && freeList >= 2 )
			// disk contents are valid
			return;
		else {
			// need to format disk
			totalBlocks = diskSize;
			format( defaultInodeBlocks );
		}
	}

	void format( int inodeSize ) {
		// 4 blocks for inodes if 64 inodes
		this.totalInodes = inodeSize;

		for(short var2 = 0; var2 < this.inodeBlocks; ++var2) {
			Inode var3 = new Inode();
			var3.flag = 0;
			var3.toDisk(var2);
		}

		this.freeList = 2 + this.inodeBlocks * 32 / 512;

		for(int var5 = this.freeList; var5 < this.totalBlocks; ++var5) {
			byte[] var6 = new byte[512];

			for(int var4 = 0; var4 < 512; ++var4) {
				var6[var4] = 0;
			}

			SysLib.int2bytes(var5 + 1, var6, 0);
			SysLib.rawwrite(var5, var6);
		}

		this.sync();
	}

	// Write bak totalBlocks, indoeBlocks, and freeList to disk
	void sync() {

	}

	// Dequeue the top block from the free list
	public int getFreeBlock() {

	}

	// Enqueue a given block to the end of the free list
	public boolean returnBlock ( int blockNumber ) {

	}
}
