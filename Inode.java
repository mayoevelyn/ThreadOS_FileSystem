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

   Inode( short iNumber ) {                       // retrieving inode from disk
      // design it by yourself.
      // from slide
      int blockNumber = 1 + iNumber / 16;
      byte[] data = new byte[Disk.blockSize];
      SysLib.rawread( blockNumber, data );
      int offset = ( iNumber % 16 ) * 32;

      length = SysLib.bytes2int( data, offset );
      offset += 4;
      count = SysLib.bytes2short( data, offset );
      offset += 2;
      flag = SysLib.bytes2short( data, offset );
      offset += 2;

      //...;
   }

   // returns void according to slide?
   int toDisk( short iNumber ) {                  // save to disk as the i-th inode
      // design it by yourself.
   }

   short getIndexBlockNumber() {

   }

   boolean setIndexBlock( short indexBlockNumber ) {

   }

   short findTargetBlock( int offset ) {
      // from slides
      // blk = offset/512;
      // if(blk >= 11) scan the index block
   }
}
