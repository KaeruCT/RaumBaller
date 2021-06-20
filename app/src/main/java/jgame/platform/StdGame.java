package jgame.platform;
import jgame.*;
import java.io.*;

/** A basic framework for a game.  It supports an animation and game timer,
* object creation at fixed intervals, score, lives, levels, configurable keys.
* There are title, start-level, next-level, death, and game-over sequences.
* Todo: highscores, key configuration file and GUI.

* <p>To initialise this class, use the regular initEngine (from main), or
* initEngineApplet (from parameterless constructor).  You can supply the width
* and height of the window as command line arguments by calling
* parseSizeArgs(args) from your main().  Define initGame() as usual.  StdGame
* does all its logic in the doFrame method, so you should ensure that it is
* called (i.e. call super.doFrame() if you choose to override doFrame).  The
* game will automatically start in the "Title" gamestate when it finds that
* it isn't in this state in the first call to doFrame().  You can also set the
* "Title" state in initGame if you even want the first frame to be in
* "Title".

* <p>The class uses the following state machine, using JGEngine's state
* machine mechanism:

* <p><i>Title</i>: displays title screen.  Transition to
* {StartLevel,StartGame} when the key_startgame is pressed.  Before the
* transition, initNewGame(), defineLevel(), and initNewLife() are called.

* <p><i>InGame</i>: game is playing.  Transition to LifeLost when lifeLost()
* is called from within the game.  Transition to LevelDone when levelDone() is
* called from within the game.  Transition to GameOver when gameOver() is
* called (i.e. to quit game).  The gametime timer indicates how many ticks the
* game has been running since the beginning of the level.

* <p>StdGame supports a set of standard game sequences, which are represented
* as game states: StartLevel/StartGame, LevelDone, LifeLost, GameOver.  These
* can be configured so they add the InGame state to the sequence state (i.e.
* the game is in both states simultaneously).  This can be used to animate the
* game while playing the corresponding sequence.  This is off by default.  The
* seqtimer timer is set to 0 at the beginning of each sequence, and increments
* during the sequence to indicate how many ticks the sequence has been
* playing.  The number of ticks that the sequence should take can be
* configured, or the sequence can be skipped altogether by setting ticks to 0.

* <p><i>StartGame</i>: start game sequence is played.  Transition to InGame
* after a certain time has elapsed or the continuegame key is pressed. 

* <p><i>StartLevel</i>: start level sequence is played.  Transition to InGame
* after a certain time has elapsed or the continuegame key is pressed.  Is always
* active in combination with StartGame; it's just an indication that StartGame
* is also a start of a new level.

* <p><i>LevelDone</i>: next level sequence is played.  Transition to
* StartLevel/StartGame after a certain time has elapsed or the continuegame key
* is pressed.  Before the transition, resp. incrementLevel() and defineLevel()
* are called.

* <p><i>LifeLost</i>:  player has just died, a death sequence is played.
* Transition to either GameOver or StartGame after a certain time has elapsed
* or the continuegame key is pressed, dependent of whether there are lives left.
* Before the transition to StartGame, decrementLives() and initNewLife are
* called.

* <p><i>GameOver</i>: game over sequence is played.  Transition to Title after
* a certain time or the continuegame key is pressed.

* <p>Built in are also game exit (through the key_quitgame, which is Escape by
* default), pause game (through the key_pausegame, which defaults to 'P'), and
* program exit (key_quitprogram, default Escape).
*/
public abstract class StdGame extends JGEngine {

	// XXX can levelDone and lifeLost be triggered simultaneously? (ramjet)

	/* settings */

	/** Flag indicating that audio is enabled */
	public boolean audioenabled=false;

	/** Flag indicating that sound enable dialog should be shown at startup */
	public boolean audio_dialog_at_startup=true;

	/** flag indicating that accelerometer set zero point menu should be
	 * active. */
	public boolean accel_set_zero_menu=false;

	/** Key for starting the game, default = space. */
	public int key_startgame = ' ';
	/** Key for invoking the game settings window, default = enter. */
	public int key_gamesettings = KeyEnter;
	/** Key for continuing the game when in a sequence, default = space. */
	public int key_continuegame = ' ';
	/** Key for quitting the current game, default = escape. */
	public int key_quitgame = KeyEsc;
	/** Key for quitting the program, default = escape. */
	public int key_quitprogram = KeyEsc;
	/** Key for pausing the game, default = P. */
	public int key_pausegame = 'P';
	/** Key for moving, default = cursors. NOTE: not all Android physical
	 * keyboards have cursor keys */
	public int key_left=KeyLeft, key_right=KeyRight,
	           key_up  =KeyUp,    key_down=KeyDown;
	/** Key for moving diagonally, default = none. */
	//public int key_upleft=0, key_downleft=0,
	//           key_upright=0, key_downright=0;
	/** Key for firing (in case there are no separate directional fire keys),
	* default=Z. */
	public int key_fire      = 'Z';
	/** Key for directional firing, default = WSAD keys */
	public int key_fireleft = 'A', key_fireright= 'D',
	           key_fireup   = 'W', key_firedown = 'S';
	/** Key for special action, default=X */
	public int key_action      = 'X';
	/** Key for diagonal firing, default = none */
	//public int key_fireupleft =0, key_firedownleft=0,
	//           key_fireupright=0, key_firedownright=0;
	/** Keys for special actions.  Default = action[0]=ctrl, action[1]=alt */
	//public int [] key_action = new int [] 
	//{ KeyCtrl,KeyAlt, 0,0,0, 0,0,0,0,0 };

	/** Game timer.  Is reset to 0 at the beginning of each level, increments
	 * with gamespeed during InGame. */
	public double gametime=0;
	/** Sequence timer.  Is reset to 0 at the start of the Title, Highscores,
	* EnterHighscore, StartLevel, StartGame,
	* LevelDone, LifeLost, GameOver sequences.  Increments with gamespeed
	* always.  Can be used to time animations for these sequences. */
	public double seqtimer=0;
	/** Animation timer.  Always increments with gamespeed.
	* Can be used to time animations etc. */
	public double timer=0;
	/** Player score; starts at 0 at beginning of game. */
	public int score=0;
	/** Difficulty level; starts at 0 at beginning of game.  Can be
	 * incremented each time a level is complete. Can be used to determine game
	 * difficulty settings throughout the game.  */
	public int level=0;
	/** Game stage, which is usually the same as level, but typically goes on
	* counting, while level may stop increasing at a certain value.
	* Can be used to vary graphic sets, display stage number, etc. */
	public int stage=0;
	/** Lives count, 0 means game over.  */
	public int lives=0;
	/** Initial value for lives; default=4 */
	public int initial_lives=4;

	/** Number of ticks to stay in StartLevel/StartGame state, 0 = skip */
	public int startgame_ticks=80;
	/** Number of ticks to stay in LevelDone state, 0 = skip */
	public int leveldone_ticks=80;
	/** Number of ticks to stay in LifeLost state, 0 = skip */
	public int lifelost_ticks=80;
	/** Number of ticks to stay in GameOver state, 0 = skip */
	public int gameover_ticks=120;

	/** Indicates whether the InGame state should be retained when in the
	 * corresponding sequence state. */
	public boolean startgame_ingame=false, leveldone_ingame=false,
	               lifelost_ingame=false, gameover_ingame=false;

	/** Horizontal margins to be used by status displays, default 12 pixels. */
	public int status_l_margin=12,status_r_margin=12;

	/** Font to use to display score */
	public JGFont status_font = new JGFont("Courier",0,12);
	/** Color to use to display score */
	public JGColor status_color = JGColor.white;
	/** Image to use to display lives */
	public String lives_img = null;

	/** Font to use to display title and messages */
	public JGFont title_font = new JGFont("Courier",0,18);
	/** Color to use to display title and messages */
	public JGColor title_color = JGColor.white;
	/** Color to use to display background effects behind title and messages */
	public JGColor title_bg_color = JGColor.blue;

	/** indicates that engine has just started and has not produced a single
	 * frame. */
	boolean just_inited=true;

	/** Set the status display variables in one go. */
	public void setStatusDisplay(JGFont status_font,JGColor status_color,
	String lives_img) {
		this.status_font=status_font;
		this.status_color=status_color;
		this.lives_img=lives_img;
	}
	/** Set all sequence variables in one go. */
	public void setSequences(boolean startgame_ingame,int startgame_ticks,
	boolean leveldone_ingame, int leveldone_ticks,
	boolean lifelost_ingame, int lifelost_ticks,
	boolean gameover_ingame, int gameover_ticks) {
		this.startgame_ingame=startgame_ingame;
		this.leveldone_ingame=leveldone_ingame;
		this.lifelost_ingame=lifelost_ingame;
		this.gameover_ingame=gameover_ingame;
		this.startgame_ticks=startgame_ticks;
		this.leveldone_ticks=leveldone_ticks;
		this.lifelost_ticks=lifelost_ticks;
		this.gameover_ticks=gameover_ticks;
	}

	/** Highscore table, null (default) means not defined.  Use setHighscores
	 * to define the table. If defined, the game will handle highscores by
	 * means of the states Highscores and EnterHighscore.  */
	public Highscore [] highscores=null;

	/** Maximum length of name typed by user. */
	public int highscore_maxnamelen=15;

	/** Player's name being entered in EnterHighscore; is reset to the empty
	 * string before the EnterHighscore state is entered.  Is altered by
	 * doFrameEnterHighscore. */
	public String playername="";


	/** Time to wait in title screen before showing highscores. */
	public int highscore_waittime=500;
	/** Time to show highscores before going back to title screen. */
	public int highscore_showtime=600;
	/** Font to use to display highscores */
	public JGFont highscore_font = new JGFont("Courier",0,16);
	/** Color to use to display highscores */
	public JGColor highscore_color = JGColor.white;
	/** Font to use to display highscore title information */
	public JGFont highscore_title_font = new JGFont("Courier",0,16);
	/** Color to use to display highscore title information */
	public JGColor highscore_title_color = JGColor.white;
	/** Title string to display above highscores */
	public String highscore_title="Highest Scores";
	/** String to display above highscore entry screen. */
	public String highscore_entry="You have a high score!";


	private YesNoDialog sound_dialog;


	class YesNoDialog {
		public boolean selection;
		public boolean selected;
	}

	double [] accelzerovector = new double[] {0,0,1};

	/** get accelerometer zero vector */
	public double [] getAccelZeroVector() {
		return accelzerovector;
	}
	/** get zero vector corrected acceleration vector (slow) */
	public double [] getAccelZeroCorrected() {
		// getAccelVec clones vector, so we do not have to copy it again
		double [] ret = getAccelVec();
		//for (int i=0; i<3; i++) ret[i] -= accelzerovector[i];
		//return ret;
		// 3D method: rotate by the difference between (0,0,1) and the
		// accelzerovector.
		// find rotation as axis-angle
		// axis = normal of {0,0,1} and accelzerovector
		// angle = angle between {0,0,1} and accelzerovector
		double [] norm = getNormal(new double [][] {
			accelzerovector, {0,0,0},  {0,0,1}} );
		// norm is zero when accelzerovector coincides with {0,0,1}
		if (length3(norm) < 0.02) return ret;
		//System.out.println("######"+norm[0]+" "+norm[1]+" "+norm[2]);
		double ang = atan3(accelzerovector, new double []  {0,0,1});
		// perform rotation
		double [][] rotm = getRotateMatrix3x3(ang,norm[0],norm[1],norm[2]);
		return rotateVector(ret,rotm);
	}

	// 3D functions from vesselviewer.Math3D

	/** Normal of two vectors given by three points, namely, P2-P1 and P1-P0
	 * (P1 can be considered the origin).
	* Vector is not normalised! */
	static double [] getNormal(double [][] p) {
		return new double [] {
				(p[2][1]-p[1][1]) * (p[1][2]-p[0][2])
			  - (p[2][2]-p[1][2]) * (p[1][1]-p[0][1]),
				(p[2][2]-p[1][2]) * (p[1][0]-p[0][0])
			  - (p[2][0]-p[1][0]) * (p[1][2]-p[0][2]),
				(p[2][0]-p[1][0]) * (p[1][1]-p[0][1])
			  - (p[2][1]-p[1][1]) * (p[1][0]-p[0][0]) };
	}

	// unused
	/** get angle between vector and (0,0,1) */
	static double atan3(double [] p) {
		double len = length3(p);
		if (len==0) return 0;
		return Math.acos(p[2]/len);
	}

	/** get angle between two vectors
	* http://www.mcasco.com/qa_ab3dv.html */
	public static double atan3(double [] p1, double [] p2) {
		double lenprod = length3(p1)*length3(p2);
		if (lenprod==0) return 0;
		double dotprod = p1[0]*p2[0] + p1[1]*p2[1] + p1[2]*p2[2];
		return Math.acos(dotprod/lenprod);
	}

	/** Get rotate matrix openGL style (vector + angle) */
	static double [][] getRotateMatrix3x3(double ang, double x,double y,double z){
		double [] [] m = new double[3][3];
		double norm = Math.sqrt(x*x + y*y + z*z);
		x /= norm;
		y /= norm;
		z /= norm;
		double c = Math.cos(ang);
		double s = Math.sin(ang);
		// see http://pyopengl.sourceforge.net/documentation/manual/glRotate.3G.html
		m[0][0] = x*x*(1-c) + c;
		m[0][1] = x*y*(1-c) - z*s;
		m[0][2] = x*z*(1-c) + y*s;

		m[1][0] = y*x*(1-c) + z*s;
		m[1][1] = y*y*(1-c) + c;
		m[1][2] = y*z*(1-c) - x*s;

		m[2][0] = x*z*(1-c) - y*s;
		m[2][1] = y*z*(1-c) + x*s;
		m[2][2] = z*z*(1-c) + c;
		return m;
	}

	public static double length3(double [] p) {
		return Math.sqrt(p[0]*p[0] + p[1]*p[1] + p[2]*p[2]);
	}

	/** multiply vector by rotation matrix */
	static double[] rotateVector(double [] vec, double [][] m) {
		double [] rot = new double [3];
		rot[0] = m[0][0]*vec[0] + m[0][1]*vec[1] + m[0][2]*vec[2];
		rot[1] = m[1][0]*vec[0] + m[1][1]*vec[1] + m[1][2]*vec[2];
		rot[2] = m[2][0]*vec[0] + m[2][1]*vec[1] + m[2][2]*vec[2];
		return rot;
	}





	/** Define highscore table. */ 
	public void setHighscores(int nr_highscores, Highscore default_hisc,
	int maxnamelen) {
		highscores = new Highscore [nr_highscores];
		for (int i=0; i<nr_highscores; i++)
			// XXX maybe clone?
			highscores[i] = default_hisc;
		highscore_maxnamelen=maxnamelen;
	}

	/** Set highscore display settings. */ 
	public void setHighscoreDisplay(int waittime,int showtime, JGFont font,
	JGColor color, String title, String entry, JGFont titlefont, JGColor titlecolor) {
		highscore_waittime=waittime;
		highscore_showtime=showtime;
		highscore_font=font;
		highscore_color=color;
		highscore_title=title;
		highscore_entry=entry;
		highscore_title_font=titlefont;
		highscore_title_color=titlecolor;
	}

	public static JGPoint parseSizeArgs(String [] args,int arg_ofs) {
		// dummy implementation
		return null;
	}

	/* special state functions */

	/** Initialise the game when a new game is started.  Default sets level,
	 * stage, score to 0, and lives to initial_lives. */
	//public void initNewGame() { initNewGame(0); }
	/** Initialise the game when a new game is started.  Default sets
	 * stage, score to 0, and lives to initial_lives. Level is set to supplied
	 * argument. */
	public void initNewGame(int level_selected) {
		level=level_selected;
		stage=level;
		score=0;
		lives=initial_lives;
	}
	/** Initialise play specifically after a new life is introduced (that is,
	 * at game start and after the player has died.  This is typically used to
	 * reinitialise the player.  If you want a specific initialisation at
	 * both the beginning of the level or after the player death, use
	 * startInGame(). Default is do nothing. */
	public void initNewLife() {}

	/** Initialise a level.  Default is do nothing. */
	public void defineLevel() {}

	/** Code for losing a life before transition from LifeLost to InGame is
	 * made.  Default is decrement lives.
	 */
	public void decrementLives() {
		lives--;
	}
	/** Code for incrementing a level before transition from LevelDone to
	* InGame is made.  Default is increment level and stage. */
	public void incrementLevel() {
		level++;
		stage++;
	}

	/** Start game at level 0 */
	public void startGame() {
		startGame(0);
	}
	/** Start game at given level */
	public void startGame(int level_selected) {
		gametime=0;
		initNewGame(level_selected);
		defineLevel();
		initNewLife();
		// code duplicated in levelDone
		clearKey(key_continuegame);
		seqtimer=0;
		if (startgame_ticks > 0) {
			setGameState("StartLevel");
			addGameState("StartGame");
			if (startgame_ingame) addGameState("InGame");
			new JGTimer(startgame_ticks,true,"StartLevel") {
				public void alarm() { setGameState("InGame"); } };
		} else {
			setGameState("InGame");
		}
	}

	/* state transition functions */

	/** Call to make state transition to LifeLost.  Is ignored when in
	 * another state than InGame or {InGame,StartLevel/StartGame}.
	 * After the LifeLost
	 * sequence, goes to InGame or GameOver, depending on lives left. */
	public final void lifeLost() {
		if (!inGameState("InGame") || inGameState("LevelDone")
		|| inGameState("LifeLost") || inGameState("GameOver") ) return;
		//	System.err.println(
		//	"Warning: lifeLost() called from other state than InGame." );
		//}
		clearKey(key_continuegame);
		removeGameState("StartLevel");
		removeGameState("StartGame");
		seqtimer=0;
		if (lifelost_ticks > 0) {
			if (lifelost_ingame) addGameState("LifeLost");
			else                 setGameState("LifeLost");
			new JGTimer(lifelost_ticks,true,"LifeLost") { public void alarm() {
				endLifeLost();
			} };
		} else {
			endLifeLost();
		}
	}
	private void endLifeLost() {
		clearKey(key_continuegame);
		decrementLives();
		if (lives <= 0) {
			gameOver();
		} else {
			initNewLife();
			seqtimer=0;
			if (startgame_ticks > 0) {
				// force call to startInGame()
				setGameState("StartGame");
				if (startgame_ingame) addGameState("InGame");
				new JGTimer(startgame_ticks,true,"StartGame") {
					public void alarm() { setGameState("InGame"); } };
			} else {
				// force call to startInGame()
				clearGameState();
				setGameState("InGame");
			}
		}
	}
	/** Call to make state transition to LevelDone.  Is ignored when state is
	 * not InGame or {Ingame,StartLevel/StartGame}. After the LevelDone
	 * sequence, it sets gametime to 0, calls
	 * incrementLevel and defineLevel, and goes to StartLevel/StartGame. */
	public final void levelDone() {
		if (!inGameState("InGame") || inGameState("LevelDone")
		|| inGameState("LifeLost") || inGameState("GameOver") ) return;
		//	System.err.println(
		//	"Warning: levelDone() called from other state than InGame." );
		//}
		clearKey(key_continuegame);
		removeGameState("StartLevel");
		removeGameState("StartGame");
		seqtimer=0;
		if (leveldone_ticks > 0) {
			if (leveldone_ingame) addGameState("LevelDone");
			else                  setGameState("LevelDone");
			new JGTimer(leveldone_ticks,true,"LevelDone") {public void alarm() {
				levelDoneToStartLevel();
			} };
		} else {
			levelDoneToStartLevel();
		}
	}
	private void levelDoneToStartLevel() {
		clearKey(key_continuegame);
		gametime=0;
		incrementLevel();
		defineLevel();
		seqtimer=0;
		if (startgame_ticks > 0) {
			// force call to startInGame
			setGameState("StartLevel");
			addGameState("StartGame");
			if (startgame_ingame) addGameState("InGame");
			new JGTimer(startgame_ticks,true,"StartLevel") {
				public void alarm() { setGameState("InGame"); } };
		} else {
			// force call to startInGame
			clearGameState();
			setGameState("InGame");
		}
	}
	/** Call to make straight transition to GameOver; is called automatically
	* by lifeLost when appropriate.  Is ignored when game state is not
	* {InGame}, {Ingame,Start*}, or LifeLost.  Will go to Title after GameOver
	* sequence.
	*/
	public final void gameOver() {
		// XXX hmm. we should check out these conditions
		if ( inGameState("GameOver")
		||  (!inGameState("InGame") && !inGameState("LifeLost")) ) return;
		//	System.err.println( "Warning: gameOver() called from other state"
		//		+" than InGame or LifeLost." );
		//}
		clearKey(key_continuegame);
		removeGameState("StartLevel");
		removeGameState("StartGame");
		removeGameState("LifeLost");
		seqtimer=0;
		if (gameover_ticks > 0) {
			if (gameover_ingame) addGameState("GameOver");
			else                 setGameState("GameOver");
			new JGTimer(gameover_ticks,true,"GameOver") {
				public void alarm() { gotoTitle(); } };
		} else {
			gotoTitle();
		}
	}
	/** Go to title or to highscore entry screen. */
	private void gotoTitle() {
		seqtimer=0;
		clearKey(key_startgame);
		// disabled until highscores are functional
		//if (highscores!=null
		//&&  Highscore.findPos(highscores,score)>=0 ) {
		//	setGameState("EnterHighscore");
		//} else {
			setGameState("Title");
		//}
	}

	/* initAppConfig not implemented */

	/** The main doFrame takes care of all the standard game actions.  If you
	* override it, you should typically call super.doFrame().  doFrame
	* increments timer, increments gametime when in InGame, quits game when
	* key_quitgame is pressed in InGame. In Title, it waits for the user to
	* press the key_startgame, then sets gametime to 0, calls initNewGame,
	* defineLevel, and goes to StartLevel. It also handles the continue_game
	* key inside the sequences, and the gamesettings and quitprogram keys in
	* Title.  It also ensures the audioenabled flag is passed to engine. */
	public void doFrame() {
		// pass audioenabled
		//if (sound_dialog!=null && sound_dialog.selected) {
		//	audioenabled = sound_dialog.selection;
		//}
		if (audioenabled) { enableAudio(); } else { disableAudio(); }
		// handle pause mode
		if (inGameState("Paused")) {
			clearKey(key_pausegame);
			// stop and remove game state on the next frame
			removeGameState("Paused");
			stop();
		}
		if (getKey(key_pausegame) && !inGameState("EnterHighscore")) {
			addGameState("Paused");
			clearKey(key_pausegame);
			wakeUpOnKey(key_pausegame);
		}
		// handle general actions
		timer += getGameSpeed();
		seqtimer += getGameSpeed();
		if (just_inited) {
			if (audio_dialog_at_startup) {
				canvas.post(new Runnable() {
					public void run() {
						currentact.showDialog(0);
					}
				});
			} else {
				audioenabled=true;
			}
			//sound_dialog=new YesNoDialog(this,"Sound Options","Enable Sound?");
			//Display.getDisplay(this).setCurrent(sound_dialog.yesNoAlert);

			setGameState("Title");
			just_inited=false;
			// init appconfig not implemented
			// load highscores
			//try {
				// we don't load highscores yet
				//Highscore [] loadedhisc = Highscore.load(new FileInputStream(
				//		getConfigPath(getClass().getName()+".hsc") ) );
				//if (loadedhisc.length > 0) { // empty file, ignore
				//	highscores=loadedhisc;
				//}
			//} catch (Exception e) {
				//do nothing, keep old highscores (which should be the
				//default highscores)
			//}
		} else if (inGameState("InGame")) {
			gametime+=getGameSpeed();
			if (getKey(key_quitgame)) gameOver();
		} else if (inGameState("Title")||inGameState("Highscores")) {
			if (getKey(key_quitprogram) && !isApplet()) {
				clearKey(key_quitprogram);
				exitEngine(null);
			}
			//if (getKey(key_gamesettings) ) { //&& appconfig!=null) {
				//appconfig.openGui();
				//clearKey(key_gamesettings);
 				//pause application until config window is closed
				//appconfig.waitCloseGui();
				//stop();
			//}
			if (getKey(key_startgame)) {
				startGame();
			}
			// disabled until highscores are functional
			//if (highscores!=null) {
			//	if (getKey(key_continuegame)) {
			//		clearKey(key_continuegame);
			//		seqtimer=0;
			//		if (inGameState("Title")) setGameState("Highscores");
			//		else                      setGameState("Title");
			//	}
			//	if (inGameState("Title") && seqtimer>=highscore_waittime) {
			//		seqtimer=0;
			//		setGameState("Highscores");
			//	} else
			//	if (inGameState("Highscores") && seqtimer>=highscore_showtime) {
			//		seqtimer=0;
			//		setGameState("Title");
			//	}
			//}
		} else if (inGameState("StartGame")) {
			if (getKey(key_continuegame)) setGameState("InGame");
		} else if (inGameState("LevelDone")) {
			if (getKey(key_continuegame)) levelDoneToStartLevel();
		} else if (inGameState("LifeLost")) {
			if (getKey(key_continuegame)) endLifeLost();
		} else if (inGameState("GameOver")) {
			if (getKey(key_continuegame)) gotoTitle();
		}
	}

	/* default doFrame... actions; note we still have to define the others.*/

	/** Default lets user type name into the variable playername.  If enter is
	* pressed, highscore is put in table and saved to disk.
	*/
	public void doFrameEnterHighscore() {
		/* this method needs to be replaced.
		char key = getLastKeyChar();
		clearLastKey();
		if (key==KeyBackspace && playername.length()>0)
			playername = playername.substring(0,playername.length()-1);
		if (key==KeyEnter) {
			highscores = Highscore.insert(highscores,
					new Highscore(score,playername));
			clearLastKey();
			clearKey(KeyEnter);
			// save not implemented
			//saveHighscores();
			seqtimer=0;
			setGameState("Highscores");
		}
		if (key>=32 && key<127 && playername.length()<highscore_maxnamelen)
			playername += key;
		*/
	}


	/* default start... functions */

	/** Initialise the title screen.  This is a standard state transition
	* function. Default is do nothing. */
	public void startTitle() {}

	/** Initialise the highscore display.  This is a standard state transition
	* function. Default is do nothing. */
	public void startHighscores() {}

	/** Initialisation at the start of the in-game action. This is a
	* standard state transition function.  Note that it is always called after
	* StartLevel and LifeLost, even if startgame_ingame and
	* lifelost_ingame are set.  Default is do nothing. */
	public void startInGame() {}

	/** Initialise start-level sequence. This is a
	* standard state transition function.  Default is do nothing. */
	public void startStartLevel() {}

	/** Initialise start-game sequence. This is a
	* standard state transition function.  Default is do nothing. */
	public void startStartGame() {}

	/** Initialise next-level sequence. This is a
	* standard state transition function.  Default is do nothing. */
	public void startLevelDone() {}

	/** Initialise death sequence. This is a
	* standard state transition function.  Default is do nothing. */
	public void startLifeLost() {}

	/** Initialise game over sequence. This is a
	* standard state transition function. Default is do nothing. */
	public void startGameOver() {}

	/** Initialise enter-highscore screen.  This is a standard state
	 * transition function.  Default is clear lastkey and set playername to
	 * the empty string.*/
	public void startEnterHighscore() {
		clearLastKey();
		playername="";
	}
	/* default paint functions */

	// only regenerate strings to display if the variables changed.
	// toString and concatenation take a lot of time.
	String scorestr="",livesstr="";
	int oldscore=-1,oldlives=-1;
	/** Default paintFrame displays score at top left, lives at top right.
	* When lives_img is set, it uses that image to display lives. */
	public void paintFrame() {
		if (score!=oldscore) {
			scorestr = "Score "+score;
			oldscore = score;
		}
		if (lives!=oldlives) {
			livesstr = "Lives "+lives;
			oldlives = lives;
		}
		setFont(status_font);
		setColor(status_color);
		drawString(scorestr,status_l_margin,0,-1);
		if (lives_img==null) {
			drawString(livesstr,viewWidth()-status_r_margin,0,1);
		} else {
			drawCount(lives-1, lives_img, viewWidth()-status_r_margin,0,
				- getImageSize(lives_img).x-2 );
		}
	}
	/** Default displays class name as title, and "press [key_startgame] to
	 * start" below it. */
	public void paintFrameTitle() {
		drawString(getClass().getName().substring(getClass().getName()
			.lastIndexOf('.' )+1 ),
			viewWidth()/2,viewHeight()/3,0,title_font,title_color);
		drawString("Press "+getKeyDesc(key_startgame)+" to start",
			viewWidth()/2,6*viewHeight()/10,0,title_font,title_color);
		drawString("Press "+getKeyDesc(key_quitprogram)+" to quit",
			viewWidth()/2,7*viewHeight()/10,0,title_font,title_color);
		// settings not (yet) supported
		//drawString("Press "+getKeyDesc(key_gamesettings)+" for settings",
		//	viewWidth()/2,7*viewHeight()/10,0,title_font,title_color);
	}
	/** The game is halted in pause mode, but the paintFrame is still done to
	* refresh the screen.  Default behaviour of paintFramePaused() is display
	* "Paused", "Press [key_pausegame] to continue" using title_font,
	* title_color */
	public void paintFramePaused() {
		setColor(title_bg_color);
		// XXX note: original height was
		// 5*viewHeight()/36+(int)getFontHeight(title_font)
		drawRect(viewWidth()/20,15*viewHeight()/36,18*viewWidth()/20,
			6*viewHeight()/36+(int)getFontHeight(title_font), true,false,false);
		drawString("Paused",viewWidth()/2,16*viewHeight()/36,0,
			title_font,title_color);
		drawString("Press "+getKeyDesc(key_pausegame)+" to continue",
			viewWidth()/2,19*viewHeight()/36,0, title_font,title_color);
	}
	/** Default displays "Level "+(stage+1). */
	public void paintFrameStartLevel() {
		drawString("Level "+(stage+1),
			viewWidth()/2,3*viewHeight()/5,0,title_font,title_color);
	}
	/** Default displays "Start !". */
	public void paintFrameStartGame() {
		drawString("Start !",
			viewWidth()/2,viewHeight()/3,0,title_font,title_color);
	}
	/** Default displays "Level Done !". */
	public void paintFrameLevelDone() {
		drawString("Level Done !",
			viewWidth()/2,viewHeight()/3,0,title_font,title_color);
	}
	/** Default displays "Life Lost !". */
	public void paintFrameLifeLost() {
		drawString("Life Lost !",
			viewWidth()/2,viewHeight()/3,0,title_font,title_color);
	}
	/** Default displays "Game Over!". */
	public void paintFrameGameOver() {
		drawString("Game Over !",
			viewWidth()/2,viewHeight()/3,0,title_font,title_color);
	}

	/** Default displays highscore_entry, and the player's score and
	 * playername currently being entered. */
	public void paintFrameEnterHighscore() {
		drawString(highscore_entry,
			viewWidth()/2,viewHeight()/3,0,highscore_title_font,
				highscore_title_color);
		drawString(""+score,
			viewWidth()/2,viewHeight()/2,0,highscore_font,highscore_color);
		drawString(playername+"|",
			viewWidth()/2,2*viewHeight()/3,0,highscore_font,highscore_color);
	}

	/** Default displays the highscore list. Fields are not yet supported.  */
	public void paintFrameHighscores() {
		drawString(highscore_title,
			viewWidth()/2,viewHeight()/7,0,highscore_title_font,
			highscore_title_color);
		double yinc = 0.7*viewHeight()/highscores.length;
		double ypos = 0.6*viewHeight() - yinc*(highscores.length/2.0);
		for (int i=0; i<highscores.length; i++) {
			drawString(""+highscores[i].score,
				0.22*viewWidth(), ypos + i*yinc,
					1,highscore_font,highscore_color);
			drawString(highscores[i].name,
				0.8*viewWidth(), ypos + i*yinc,
					1,highscore_font,highscore_color);
		}
	}

	/* handy game functions */

	/** Returns true every increment ticks, but only when gametime is between
	* min_time and max_time. */
	public boolean checkTime(int min_time,int max_time,int increment) {
		return gametime>min_time && gametime<max_time
		       && (((int)gametime-1)%increment)<(int)getGameSpeed();
	}
	/** Returns true every increment ticks. */
	public boolean checkTime(int increment) {
		return (((int)gametime-1)%increment)<(int)getGameSpeed();
	}

	/* handy draw and effects functions */

	/** Draw a row of objects to indicate the value count.  This is typically
	 * used to indicate lives left. */
	public void drawCount(int count, String image,int x,int y,int increment_x) {
		if (increment_x < 0) x += increment_x;
		for (int i=0; i<count; i++)
			drawImage(x + i*increment_x, y, image, false);
	}
	/** Draw a string with letters that move up and down individually. */
	public void drawWavyString(String s, int x,int y,int align,int increment_x,
	double tmr,double amplitude, double pos_phaseshift, double timer_phaseshift,
	JGFont font, JGColor col) {
		setFont(font);
		setColor(col);
		if (align==0) {
			x -= increment_x*s.length()/2;
		} else if (align==1) {
			x -= increment_x*s.length();
		}
		for (int i=0; i<s.length(); i++)
			drawString(s.substring(i,i+1), x + i*increment_x,
				y + (int)(amplitude * 
				-Math.cos(Math.PI*(pos_phaseshift*i + tmr*timer_phaseshift))
				), 0);
	}

	/** Draw a String that zooms in and out. Alignment is always center. Note
	 * that tmr = 0 will start the font zooming in. */
	//public void drawZoomString(String s,int x,int y,
	//int tmr, double min_size_fac, double speed, Font font, JGColor col) {
	//	drawString(s,x,y,0,zoomed,col);
	//}

	/** Get font for zooming text in and out. Note that tmr = 0 will start
	* the font zooming in from the farthest position. */
	public JGFont getZoomingFont(JGFont basejgfont, double tmr,
	double min_size_fac, double speed) {
		// dummy implementation
		return basejgfont;
	}

	/** Get a colour from a colour cycle. */
	public JGColor cycleColor(JGColor [] cycle, double tmr, double speed) {
		return cycle[ ( (int)(tmr*speed) ) % cycle.length ];
	}

	/** Walk across the screen, standing still momentarily at a specific
	* position. */
	public int posWalkForwards(int begin_pos, int end_pos, double tmr,
	int end_time,int standstill_pos,int standstill_time,int standstill_count){
		if (tmr < standstill_time) {
			double step = (standstill_pos - begin_pos)/(double)standstill_time;
			return begin_pos + (int)(tmr*step);
		} else if (tmr>=standstill_time&&tmr<standstill_time+standstill_count){
			return standstill_pos;
		} else if (tmr >= standstill_time+standstill_count && tmr < end_time) {
			int beg2_time = standstill_time + standstill_count;
			double step=(end_pos-standstill_pos)/(double)(end_time - beg2_time);
			return standstill_pos + (int)((tmr-beg2_time) * step);
		} else {
			return end_pos;
		}
	}
}
