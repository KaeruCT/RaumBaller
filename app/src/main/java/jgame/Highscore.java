package jgame;
import java.util.*;
import java.io.*;
/** Class for handling highscores.  A highscore consists of a score, a name,
 * and optionally, a number of information fields such as level, etc. */
public class Highscore {
	public int score;
	public String name;
	public String [] fields=null;

	/** Construct regular highscore */
	public Highscore(int score,String name) {
		this.score=score;
		this.name=name;
	}

	/** Construct highscore with one extra field */
	public Highscore(int score,String name,String field1) {
		this.score=score;
		this.name=name;
		this.fields=new String[] { field1 };
	}

	/** Construct highscore with extra fields */
	public Highscore(int score,String name,String field1,String field2) {
		this.score=score;
		this.name=name;
		this.fields=new String[] { field1,field2 };
	}

	/** Construct highscore with extra fields */
	public Highscore(int score,String name,String field1,String field2,
	String field3) {
		this.score=score;
		this.name=name;
		this.fields=new String[] { field1,field2,field3 };
	}

//	class HighscoreComparator implements Comparator {
//		public HighscoreComparator() {}
//		public int compare(Object o1,Object o2) {
//			Highscore h1 = (Highscore) o1;
//			Highscore h2 = (Highscore) o2;
//			// descending order
//			return h2.score-h1.score;
//		}
//		public boolean equals(Object obj) {
//			return obj instanceof HighscoreComparator;
//		}
//	}
//
//	/** Sort given highscores.  Leaves old array untouched. */
//	public static Highscore [] sort(Highscore [] highscores) {
//		ArrayList sorted = new ArrayList(highscores.length);
//		for (int i=0; i<highscores.length; i++) {
//			sorted.addElement(highscores[i]);
//		}
//		Collections.sort(sorted,highscores[0].new HighscoreComparator());
//		return (Highscore [])sorted.toArray(new Highscore[] {});
//	}

	/** Find position (array index) of given score in highscore list,
	 * -1 means not in highscores. 
	 * @param highscores  sorted highscore list
	 * @return  0 means first position, etc.  -1 means not in list
	 */
	public static int findPos(Highscore [] highscores, int newscore) {
		for (int i=0; i<highscores.length; i++) {
			if (newscore > highscores[i].score) return i;
		}
		return -1;
	}

	/** Try to insert new highscore in given highscore list.  Do nothing if
	 * it's too low.  Leaves old array untouched. */
	public static Highscore [] insert(Highscore [] highscores,
	Highscore newscore) {
		if (findPos(highscores,newscore.score) >= 0) {
			highscores[highscores.length-1] = newscore;
			return sort(highscores);
		} else {
			return highscores;
		}
	}

	public static Highscore [] load(InputStream in)
	throws IOException {
		Vector highscores=new Vector(20,40);
		InputStreamReader inr = new InputStreamReader(in);
		String line;
		while ( (line=jgame.impl.EngineLogic.readline(inr)) != null ) {
			Vector fields = new Vector(5,10);
			// XXX we use "`" to represent empty string because
			// StringTokenizer skips empty tokens
			Vector tokens = jgame.impl.EngineLogic.tokenizeString(line,'\t');
			//StringTokenizer toker = new StringTokenizer(line,"\t");
			for (Enumeration e=tokens.elements(); e.hasMoreElements(); ) {
				String tok = (String)e.nextElement();
				if (tok.equals("`")) tok="";
				fields.addElement(tok);
			}
			Highscore hs=null;
			if (fields.size()==1) {
				// we assume we have a highscore with no fields and empty name
				hs=new Highscore(Integer.parseInt((String)fields.elementAt(0)), "");
			}
			if (fields.size() >= 2) {
				hs=new Highscore(Integer.parseInt((String)fields.elementAt(0)),
					(String)fields.elementAt(1) );
			}
			if (fields.size() >= 3) {
				hs.fields=new String[fields.size()-2];
				for (int i=2; i<fields.size(); i++) {
					hs.fields[i-2] = (String)fields.elementAt(i);
				}
			}
			highscores.addElement(hs);
		}
		Highscore [] ret = new Highscore [highscores.size()];
		for (int i=0; i<highscores.size(); i++) {
			ret[i] = (Highscore)highscores.elementAt(i);
		}
		return ret;
		//return (Highscore[])highscores.toArray(new Highscore[]{});
	}

	public static void save(Highscore [] highscores,OutputStream out)
	throws IOException {
		PrintStream outp = new PrintStream(out);
		for (int i=0; i<highscores.length; i++) {
			outp.print(highscores[i].score);
			outp.print('\t');
			if (highscores[i].name.equals("")) {
				outp.print("`");//XXX backtick indicates empty string
			} else {
				outp.print(highscores[i].name);
			}
			if (highscores[i].fields!=null) {
				for (int j=0; j<highscores[i].fields.length; j++) {
					outp.print('\t');
					outp.print(highscores[i].fields[j]);
				}
			}
			outp.println();
		}
		outp.flush();
	}


	/* sorting */


	static Highscore [] sort(Highscore a[]) {
		/* index value of last element in array */
	  	int index;

		int temp;

		index = a.length - 1;
 		/* Heapify loop*/
		temp = (int)((index-1) / 2);
		for (int i=temp; i>=0; i--) {/* index of root value */
			BubbleDown(index, i, a);
		}
		/* DeleteMax loop */
		while (index > 0) {
			swap(a,0,index);
			index--;
			BubbleDown(index,0,a);
		}

		return a;
	}

	static private void BubbleDown(int index, int i, Highscore a[]) {
		int child;

		child = ((i+1)*2)-1;
		if ( (child < index) && a[child+1].score < a[child].score ) 
              child++;
		if ( (child <= index) && a[i].score > a[child].score ) {
			swap(a,i,child);
			BubbleDown(index,child,a);
		}
	}

	/**
	 * This method will be called to
	 * swap two elements in an array of strings.
	 */
	static private void swap(Highscore a[], int i, int j) {
		Highscore T;
		T = a[i];
		a[i] = a[j];
		a[j] = T;
	}


}
