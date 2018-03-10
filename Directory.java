public class Directory {
	private static int maxChars = 30; // max characters of each file name

	// Directory entries
	private int fsizes[];        // each element stores a different file size.
	private char fnames[][];    // each element stores a different file name.
	
	//Instantiates directory structure
	//
	public Directory( int maxInumber ) { // directory constructor
		fsizes = new int[maxInumber];     // maxInumber = max files
		for ( int i = 0; i < maxInumber; i++ ) 
			fsizes[i] = 0;                 // all file size initialized to 0
		
		fnames = new char[maxInumber][maxChars];
		String root = "/";                // entry(inode) 0 is "/"
		fsizes[0] = root.length( );        // fsizes[0] is the size of "/".
		root.getChars( 0, fsizes[0], fnames[0], 0 ); // fnames[0] includes "/"
	}

	//converts the byte representation of the directory into a usable list of file names and
	//	and file name lengths
	//	(Provided in slides)
	public void bytes2directory( byte data[] ) {
		int offset = 0;
		for(int i = 0; i < fsizes.length; i++, offset += 4) {
			fsizes[i] = SysLib.bytes2int( data, offset );
		}
		
		for(int i = 0; i < fnames.length; i++, offset += maxChars * 2) {
			String fname = new String( data, offset, maxChars * 2);
			fname.getChars(0, fsizes[i], fnames[i], 0);
		}
	}

	//converts the important directory info back to bytes for storage
	//	essentially the reverse of bytes2directory
	public byte[] directory2bytes( ) {
		int size = fsizes.length * 4 + fnames.length * maxChars * 2;
		byte[] directoryAsBytes = new byte[size];

		//add file sizes to our array
		int offset = 0;
		for(int i = 0; i < fsizes.length; i++) {
			SysLib.int2bytes(fsizes[i], directoryAsBytes, offset);
			offset += 4;
		}

		for(int i = 0; i < fnames.length; i++) {
			String tempFileName = new String(fnames[i],0,fsizes[i]);
			byte[] byteFileName = tempFileName.getBytes();
			System.arraycopy(byteFileName, 0, directoryAsBytes, offset, byteFileName.length);
			offset += maxChars * 2;
		}

		return directoryAsBytes;
	}


	//allocates an iNode for a new file
	public short ialloc( String filename ) {
		for(int i = 1; i < fsizes.length; i++) {
			if(fsizes[i] == 0) {
			   //get length of file name, truncate at max
			   fsizes[i] = Math.min(maxChars,filename.length());
			   
			   //copy byte form of file name into fnames index
			   filename.getChars(0,fsizes[i],fnames[i],0);
			   
			   //return the iNode we used
			   return (short)i;
			}
		}
		return -1;
	}

	//frees up an iNode by setting its file size to 0
	//	returns true if iNode was freed, or false if it was
	//	already free
	public boolean ifree( short iNumber ) {
		if (fsizes[iNumber] > 0) {
			fsizes[iNumber] = 0;
			return true;
		}
		return false;
	}

	//returns the iNode number for a given filename
	//	or -1 if no file with that name exists
	public short namei( String filename ) {
		for(int i = 0; i < fsizes.length; i++) {
			if(fsizes[i] > 0 && fsizes[i] == filename.length()) {
				String compare = new String(fnames[i], 0, fsizes[i]);
				if(filename.equals(compare)) {
					return (short)i;
				}
			}
		}
		return -1;
	}

}