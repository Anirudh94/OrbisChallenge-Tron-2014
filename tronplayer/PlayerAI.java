import java.awt.Point;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import com.orbischallenge.tron.api.PlayerAction;
import com.orbischallenge.tron.client.api.LightCycle;
import com.orbischallenge.tron.client.api.TronGameBoard;
import com.orbischallenge.tron.client.api.TileTypeEnum;
import com.orbischallenge.tron.protocol.TronProtocol;
import com.orbischallenge.tron.protocol.TronProtocol.PowerUpType;
import com.orbischallenge.tron.protocol.TronProtocol.Direction;

public class PlayerAI implements Player {
	
	private Random randomMovePicker;
	private int randMove;
	boolean isFirstTurn = true;
	long avg=0;
	int n=0;
	int maxLength = 0;
	
	public void newGame(TronGameBoard map,  
			LightCycle playerCycle, LightCycle opponentCycle) {
		
		randomMovePicker = new Random();
		return;
		
	}
	
	public PlayerAction getMove(TronGameBoard map,
			LightCycle playerCycle, LightCycle opponentCycle, int moveNumber) {
		Direction final_direction = Direction.UP;
		Direction final_direction_bomb = Direction.UP;
		boolean final_has_powerup = false;
		GameMap my_map = new GameMap(map);
		GameMap bomb_map = new GameMap(map);
		
		Point current_position = playerCycle.getPosition();//playerCycle.getPosition(); # 2 element integer array
		int x_coordinate = current_position.x; // integer value
		int y_coordinate = current_position.y; // integer value

		Direction my_direction = playerCycle.getDirection();// PlayerDir.UP PlayerDir.DOWN PlayerDir.RIGHT PlayerDir.LEFT
		
		long startTime = System.nanoTime();
		
		boolean has_powerup = playerCycle.hasPowerup(); // boolean value
		boolean use_powerup = false;
		boolean is_invincible = playerCycle.isInvincible();// boolean value
		PowerUpType powerup_type = PowerUpType.SPEED;
		if(has_powerup){
		         powerup_type = playerCycle.getPowerup();
		}
		if(isFirstTurn){
			isFirstTurn = false;
			this.maxLength = 3 * map.length();
			//return DirToAction(Direction.UP,has_powerup);
		}
		
		intWrapper check = new intWrapper();
		intWrapper check_bomb = new intWrapper();
		TrailLengths trails = new TrailLengths();
		TrailLengths trails_bomb = new TrailLengths();
		my_map.resetOver(10);
		final_direction = my_map.whichPath(current_position, my_direction, this.maxLength,check, trails, true, map, 10000);
		
		if(has_powerup && powerup_type.equals(PowerUpType.BOMB)){
			System.out.println("CARRYING BOMB");
			bomb_map.placeBomb(current_position, my_direction);
			final_direction_bomb = bomb_map.whichPath(current_position, my_direction, this.maxLength,check_bomb, trails_bomb, false, map, 10000);
			int max_bomb_trail = max(trails_bomb.up, max(trails_bomb.left,trails_bomb.right));
			int max_trail = max(trails.up, max(trails.left,trails.right));
			
			if(max_trail <= 10 && max_bomb_trail >= 5*max_trail/4){
				final_direction = final_direction_bomb;
				use_powerup = true;
			}
			else if(max_bomb_trail >= 5*max_trail/2){
				final_direction = final_direction_bomb;
				use_powerup = true;
			}
		}
		
		
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/1000000; 
		
		this.avg = (duration + this.avg* this.n)/ (this.n+1);
		this.n++;
		
		//Print TIMING DATA
		//System.out.println("duration: " + this.avg);
		
		
		return DirToAction(final_direction,use_powerup);
		/*
		if(!isCellInDirWALL(map, current_position, my_direction)){
			//continue in current direction
			
			return DirToAction(final_direction,has_powerup);
		}
		if(!isCellInDirWALL(map, current_position, turnDir(my_direction,true))){
			//continue in current direction
			return DirToAction(turnDir(my_direction,true),has_powerup);
		}
		if(!isCellInDirWALL(map, current_position, turnDir(my_direction,false))){
			//continue in current direction
			return DirToAction(turnDir(my_direction,false),has_powerup);
		}
		*/
		/* DEFAULTS
		 * 
		randMove = randomMovePicker.nextInt(5);
		
		if(randMove == 0){
			return PlayerAction.SAME_DIRECTION;
		}else if(randMove == 1){
			return PlayerAction.MOVE_RIGHT;
		}else if(randMove == 2){
			return PlayerAction.MOVE_UP;
		}else if(randMove == 3){
			return PlayerAction.MOVE_LEFT;
		}else if(randMove == 4){
			return PlayerAction.MOVE_DOWN;
		}
		*/
		
		//return DirToAction(final_direction,final_has_powerup);
	}
	
	public static PlayerAction DirToAction(Direction currDir, boolean activatePowerUp){
		if(activatePowerUp){
			if(currDir == Direction.UP)
				return PlayerAction.ACTIVATE_POWERUP_MOVE_UP;
			else if(currDir == Direction.RIGHT)
				return PlayerAction.ACTIVATE_POWERUP_MOVE_RIGHT;
			else if(currDir == Direction.DOWN)
				return PlayerAction.ACTIVATE_POWERUP_MOVE_DOWN;
			else //if(currDir == Direction.LEFT)
				return PlayerAction.ACTIVATE_POWERUP_MOVE_LEFT;
		}
		else {
			if(currDir == Direction.UP)
				return PlayerAction.MOVE_UP;
			else if(currDir == Direction.RIGHT)
				return PlayerAction.MOVE_RIGHT;
			else if(currDir == Direction.DOWN)
				return PlayerAction.MOVE_DOWN;
			else //if(currDir == Direction.LEFT)
				return PlayerAction.MOVE_LEFT;
		}
		
	}
	
	public static boolean isOccupiedPoint(TronGameBoard map, Point current){
		return map.isOccupied(current.x, current.y);
	}
	
	public static TileTypeEnum typeOfCell(TronGameBoard map, Point current){
		return map.tileType(current.x, current.y);
		
	}
	
	public static boolean isOccupiedPointType(TronGameBoard map, Point current,TileTypeEnum type ){
		return map.tileType(current.x, current.y).equals(type);       // true
	}
	
	public static boolean isWall(TronGameBoard map, Point current,TileTypeEnum type ){
		return (isOccupiedPointType(map,current, TileTypeEnum.WALL) ||
				isOccupiedPointType(map,current, TileTypeEnum.TRAIL) ||
				isOccupiedPointType(map,current, TileTypeEnum.LIGHTCYCLE));
	}
	
	public static TileTypeEnum typeOfCellInDir(TronGameBoard map, Point current, Direction currDir){
		//check UP
		if(currDir == Direction.DOWN){
			return typeOfCell(map,new Point(current.x, current.y+1));
		}
		else if(currDir == Direction.LEFT){
			return typeOfCell(map,new Point(current.x-1, current.y));
		}
		else if(currDir == Direction.UP){
			return typeOfCell(map,new Point(current.x, current.y-1));
		}
		else{
			//if RIGHT
			return typeOfCell(map,new Point(current.x+1, current.y));
		}
	}
	
	public static boolean isTypeWALL(TileTypeEnum type){
		return( type.equals(TileTypeEnum.WALL)	||
				type.equals(TileTypeEnum.TRAIL)	||
				type.equals(TileTypeEnum.LIGHTCYCLE));
	}
	
	public static boolean isCellInDirWALL(TronGameBoard map, Point current, Direction currDir){
		return isTypeWALL(typeOfCellInDir(map,current,currDir));
	}
	
	public static Point CellInDir(Point pos, Direction currDir){
		if(currDir == Direction.UP)
			return new Point(pos.x,pos.y-1);
		else if(currDir == Direction.LEFT)
			return new Point(pos.x -1,pos.y);
		else if(currDir == Direction.DOWN)
			return new Point(pos.x,pos.y+1);
		else //if(currDir == Direction.RIGHT)
			return new Point(pos.x + 1,pos.y);
	}
	
	public static Direction turnDir(Direction currDir, boolean left){
		
		if(left){
			if(currDir == Direction.UP)
				return Direction.LEFT;
			else if(currDir == Direction.LEFT)
				return Direction.DOWN;
			else if(currDir == Direction.DOWN)
				return Direction.RIGHT;
			else //if(currDir == Direction.RIGTH)
				return Direction.UP;
		}
		else {
			//if RIGHT
			if(currDir == Direction.UP)
				return Direction.RIGHT;
			else if(currDir == Direction.LEFT)
				return Direction.UP;
			else if(currDir == Direction.DOWN)
				return Direction.LEFT;
			else
				return Direction.DOWN;
		}
			
	}
	
	public static int max(int a, int b){
		if(a>b)
			return a;
		else 
			return b;
	}
	
	public static int[][] ConstructMap(TronGameBoard map){
		//-1 = PowerUp
		//0 = Empty
		//1 = WALL
		//2 = TRAIL
		//3 = LIGHTCYCLE
		int[][] newMap = new int[map.length()][map.length()];
		for(int i = 0; i<map.length(); i++){
			for(int j = 0; j<map.length(); j++){
				if(typeOfCell(map,new Point(i,j)).equals(TileTypeEnum.WALL)){
					newMap[i][j] = 1;
				}
				else if(typeOfCell(map,new Point(i,j)).equals(TileTypeEnum.TRAIL)){
					newMap[i][j] = 2;
				}
				else if(typeOfCell(map,new Point(i,j)).equals(TileTypeEnum.LIGHTCYCLE)){
					newMap[i][j] = 3;
				}
				else if(typeOfCell(map,new Point(i,j)).equals(TileTypeEnum.POWERUP)){
					newMap[i][j] = -1;
				}
			}
		}
		
		return newMap;
	}

}

/**

8888888 8888888888 8 888888888o.      ,o888888o.     b.             8 
      8 8888       8 8888    `88.  . 8888     `88.   888o.          8 
      8 8888       8 8888     `88 ,8 8888       `8b  Y88888o.       8 
      8 8888       8 8888     ,88 88 8888        `8b .`Y888888o.    8 
      8 8888       8 8888.   ,88' 88 8888         88 8o. `Y888888o. 8 
      8 8888       8 888888888P'  88 8888         88 8`Y8o. `Y88888o8 
      8 8888       8 8888`8b      88 8888        ,8P 8   `Y8o. `Y8888 
      8 8888       8 8888 `8b.    `8 8888       ,8P  8      `Y8o. `Y8 
      8 8888       8 8888   `8b.   ` 8888     ,88'   8         `Y8o.` 
      8 8888       8 8888     `88.    `8888888P'     8            `Yo
      
                                Quick Guide
                --------------------------------------------

        1. THIS IS THE ONLY .JAVA FILE YOU SHOULD EDIT THAT CAME FROM THE ZIPPED STARTER KIT
        
        2. Any external files should be accessible from this directory

        3. newGame is called once at the start of the game if you wish to initialize any values
       
        4. getMove is called for each turn the game goes on

        5. map represents the game field. map.isOccupied(2, 2) returns whether or not something is at position (2, 2)
        								  map.tileType(2, 2) will tell you what is at (2, 2). A TileTypeEnum is returned.
        
        6. playerCycle is your lightcycle and is what the turn you respond with will be applied to.
                playerCycle.getPosition() is a Point object representing the (x, y) position
                playerCycle.getDirection() is the direction you are travelling in. can be compared with Direction.DIR where DIR is one of UP, RIGHT, DOWN, or LEFT
                playerCycle.hasPowerup() is a boolean representing whether or not you have a powerup
                playerCycle.isInvincible() is a boolean representing whether or not you are invincible
                playerCycle.getPowerupType() is what, if any, powerup you have
        
        7. opponentCycle is your opponent's lightcycle.

        8. You ultimately are required to return one of the following:
                                                PlayerAction.SAME_DIRECTION
                                                PlayerAction.MOVE_UP
                                                PlayerAction.MOVE_DOWN
                                                PlayerAction.MOVE_LEFT
                                                PlayerAction.MOVE_RIGHT
                                                PlayerAction.ACTIVATE_POWERUP
                                                PlayerAction.ACTIVATE_POWERUP_MOVE_UP
                                                PlayerAction.ACTIVATE_POWERUP_MOVE_DOWN
                                                PlayerAction.ACTIVATE_POWERUP_MOVE_LEFT
                                                PlayerAction.ACTIVATE_POWERUP_MOVE_RIGHT
      	
     
        9. If you have any questions, contact challenge@orbis.com
        
        10. Good luck! Submissions are due Sunday, September 21 at noon. 
            You can submit multiple times and your most recent submission will be the one graded.
 */