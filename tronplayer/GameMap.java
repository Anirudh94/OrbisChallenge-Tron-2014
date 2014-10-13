import java.awt.Point;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;

import org.hamcrest.core.IsNot;

import com.orbischallenge.tron.api.PlayerAction;
import com.orbischallenge.tron.client.api.LightCycle;
import com.orbischallenge.tron.client.api.TronGameBoard;
import com.orbischallenge.tron.client.api.TileTypeEnum;
import com.orbischallenge.tron.protocol.TronProtocol;
import com.orbischallenge.tron.protocol.TronProtocol.PowerUpType;
import com.orbischallenge.tron.protocol.TronProtocol.Direction;

public class GameMap {
	int[][] map;
	int over;
	
	public GameMap(TronGameBoard map){
		//-1 = PowerUp
		//0 = Empty
		//1 = WALL
		//2 = TRAIL
		//3 = LIGHTCYCLE
		this.map = new int[map.length()][map.length()];
		for(int i = 0; i<map.length(); i++){
			for(int j = 0; j<map.length(); j++){
				if(PlayerAI.typeOfCell(map,new Point(i,j)).equals(TileTypeEnum.WALL)){
					this.map[i][j] = 1;
				}
				else if(PlayerAI.typeOfCell(map, new Point(i,j)).equals(TileTypeEnum.TRAIL)){
					this.map[i][j] = 2;
				}
				else if(PlayerAI.typeOfCell(map, new Point(i,j)).equals(TileTypeEnum.LIGHTCYCLE)){
					this.map[i][j] = 3;
				}
				else if(PlayerAI.typeOfCell(map, new Point(i,j)).equals(TileTypeEnum.POWERUP)){
					this.map[i][j] = -1;
				} else {
					this.map[i][j] = 0;
				}
			}
		}
	}
	
	public int typeOfCell(Point current){
		return this.map[current.x][current.y];	
	}
	
	public int RecursiveCheckPath(Point currPos, Direction currDir, int trailLength, intWrapper maxTrailLength, int maxLength, intWrapper check, int checkMax){
		check.n++;

		if(outOfBounds(currPos))
			return 0;
		
		if(check.n > checkMax)
			return -1;
		
		if(trailLength > maxTrailLength.n){
			maxTrailLength.n=trailLength;
		}
		
		
		//mark current cell as visited
		this.map[currPos.x][currPos.y] = 2;
		
		int UPLength = 0;
		int RIGTHLength = 0;
		int LEFTLength = 0;

		//check radius for powerup
		
		//check UP
		if(CheckAvailableDir(PlayerAI.CellInDir(currPos, currDir)))
			UPLength = RecursiveCheckPath(PlayerAI.CellInDir(currPos, currDir), currDir, ++trailLength, maxTrailLength,maxLength,check, checkMax);
		if(UPLength<0){
			this.map[currPos.x][currPos.y] = 0;
			return -1;
		}
		Direction leftDir = PlayerAI.turnDir(currDir,true);
		if(CheckAvailableDir(PlayerAI.CellInDir(currPos, leftDir)))
			LEFTLength = RecursiveCheckPath(PlayerAI.CellInDir(currPos, leftDir),leftDir, ++trailLength, maxTrailLength, maxLength,check, checkMax);
		if(LEFTLength<0){
			this.map[currPos.x][currPos.y] = 0;
			return -1;
		}
		Direction rightDir = PlayerAI.turnDir(currDir,false);
		if(CheckAvailableDir(PlayerAI.CellInDir(currPos, rightDir)))
			RIGTHLength = RecursiveCheckPath(PlayerAI.CellInDir(currPos, rightDir), rightDir, ++trailLength, maxTrailLength, maxLength,check, checkMax);
		if(RIGTHLength<0){
			this.map[currPos.x][currPos.y] = 0;
			return -1;
		}
		this.map[currPos.x][currPos.y] = 0;
		return 0;
					
	}
	
	public boolean checkForPowerup(){
		for(int i=0; i<this.map.length; i++){
			for(int j=0; j<this.map.length; j++){
				if(this.map[i][j] < 0)
					return true;
			}
		}
		return false;
	}
	
	public void resetOver(int n){
		this.over=n;
	}
	
	public Direction cloneDirection(Direction dirOld){
		if(dirOld.equals(Direction.DOWN))
			return Direction.DOWN;
		else if(dirOld.equals(Direction.LEFT))
			return Direction.LEFT;
		else if(dirOld.equals(Direction.RIGHT))
			return Direction.RIGHT;
		//if(dirOld.equals(Direction.UP))
		return Direction.UP;
		
		
	}
	
	public Point findEnemy(){
		for(int i =0; i < this.map.length; i++){
			for( int j = 0; j < this.map.length; j++){
				if(this.map[i][j]==3){
					return new Point(i,j);
				}
				
			}
		}
		return new Point(-1, -1);
	}
	
	public Direction whichPath(Point currPos, Direction currDir, int maxLength, intWrapper check, TrailLengths trails, boolean isMain, TronGameBoard tronMap, int checkMax){
		//mark current cell as visited
		this.map[currPos.x][currPos.y] = 2;
		Point enemyPos = findEnemy();
		if(!outOfBounds(enemyPos))
			takeEnemy(enemyPos);
		
		intWrapper UPLength = new intWrapper();
		intWrapper RIGTHLength = new intWrapper();
		intWrapper LEFTLength = new intWrapper();

		Point copyPos = new Point(currPos.x,currPos.y);
		Point powerupPoint = bfs(copyPos,cloneDirection(currDir));
		
		//System.out.println("PowerUp: (" + powerupPoint.x + " , " + powerupPoint.y + ")");
		Direction powerupDir = getDirFromPoint(currPos, powerupPoint);
		
		//check UP
		if(CheckAvailableDir(PlayerAI.CellInDir(currPos, currDir)))
			RecursiveCheckPath(PlayerAI.CellInDir(currPos, currDir), currDir, 0, UPLength, maxLength,new intWrapper(),checkMax);

		
		//System.out.println("check: " + check.n);
		Direction leftDir = PlayerAI.turnDir(currDir,true);
		if(CheckAvailableDir(PlayerAI.CellInDir(currPos, leftDir)))
			RecursiveCheckPath(PlayerAI.CellInDir(currPos, leftDir),leftDir, 0, LEFTLength, maxLength,new intWrapper(),checkMax);
		
	
		Direction rightDir = PlayerAI.turnDir(currDir,false);
		if(CheckAvailableDir(PlayerAI.CellInDir(currPos, rightDir)))
			RecursiveCheckPath(PlayerAI.CellInDir(currPos, rightDir), rightDir, 0, RIGTHLength, maxLength,new intWrapper(),checkMax);
		
		//System.out.println("UP: "+ UPLength.n +" LEFt: "+ LEFTLength.n +" RIGHT: " + RIGTHLength.n +" max: "+maxLength);
		
		int powerLength = 0;
		if(powerupDir.equals(currDir))
			powerLength = UPLength.n;
		else if(powerupDir.equals(leftDir))
			powerLength = LEFTLength.n;
		else
			powerLength = RIGTHLength.n;
	
		this.map[currPos.x][currPos.y] = 0;
		
		trails.right = RIGTHLength.n;
		trails.left = LEFTLength.n;
		trails.up = UPLength.n;
		
		/*
		if(isMain){
			GameMap checkMap = new GameMap(tronMap);
			Point enemyPos = checkMap.findEnemy();
		}
		*/
		
		//Check power sensitivity
		if(powerLength >= PlayerAI.max(UPLength.n, PlayerAI.max(LEFTLength.n,RIGTHLength.n))/3 &&
				CheckAvailableDir(PlayerAI.CellInDir(currPos, powerupDir))){
			return powerupDir;
		}
		
		if(UPLength.n >= RIGTHLength.n && UPLength.n >= LEFTLength.n)
			return currDir;
		if(RIGTHLength.n >= UPLength.n && RIGTHLength.n >= LEFTLength.n)
			return PlayerAI.turnDir(currDir, false);
		//if(LEFTLength >= RIGTHLength && LEFTLength >= UPLength)
			return PlayerAI.turnDir(currDir, true);		
	}
	
	public void takeEnemy(Point enemyPos){
		Point enemyMove = PlayerAI.CellInDir(enemyPos, Direction.UP);

		if(!outOfBounds(enemyMove)){
			this.map[enemyMove.x][enemyMove.y]=2;
		}
		enemyMove =  PlayerAI.CellInDir(enemyPos, Direction.DOWN);
		if(!outOfBounds(enemyMove)){
			this.map[enemyMove.x][enemyMove.y]=2;
		}
		enemyMove =  PlayerAI.CellInDir(enemyPos, Direction.LEFT);
		if(!outOfBounds(enemyMove)){
			this.map[enemyMove.x][enemyMove.y]=2;
		}
		enemyMove =  PlayerAI.CellInDir(enemyPos, Direction.RIGHT);
		if(!outOfBounds(enemyMove)){
			this.map[enemyMove.x][enemyMove.y]=2;
		}

	}
	
	public void clearLine(Point currPos, Direction currDir, int len){
		Point lineStart = currPos;
		for(int i =0; i < len; i++){
			if(!outOfBounds(lineStart)){
				if(this.map[lineStart.x][lineStart.y] != 1)
					this.map[lineStart.x][lineStart.y] = 0;
			}
			lineStart = PlayerAI.CellInDir(lineStart, currDir);
		}
	}
	
	public int placeBomb(Point currPos, Direction currDir){
		
		Point lineStart = PlayerAI.CellInDir(currPos, currDir);;
		clearLine(lineStart,currDir,4);
		
		Point leftSide = PlayerAI.CellInDir(currPos, PlayerAI.turnDir(currDir, true)); 
		clearLine(leftSide, currDir, 5);
		Point leftSideSuper = PlayerAI.CellInDir(leftSide , PlayerAI.turnDir(currDir, true)); 
		clearLine(leftSideSuper, currDir, 5);
		
		Point rigthSide = PlayerAI.CellInDir(currPos, PlayerAI.turnDir(currDir, false)); 
		clearLine(rigthSide, currDir, 5);
		Point rightSideSuper = PlayerAI.CellInDir(rigthSide , PlayerAI.turnDir(currDir, false)); 
		clearLine(rightSideSuper, currDir, 5);
		
		return 0;
	}
	
	public Direction getDirFromPoint(Point currPos,Point nextPos){
		if(nextPos.y < currPos.y)
			return Direction.UP;
		else if(nextPos.y > currPos.y)
			return Direction.DOWN;
		else if(nextPos.x < currPos.x)
			return Direction.LEFT;
		else //if(nextPos.x > currPos.x)
			return Direction.RIGHT;
	}
	
	public boolean outOfBounds(Point p){
		return p.x<0 || p.y < 0 || p.x >= this.map.length || p.y >= this.map.length; 
	}
	
	public Point bfs(Point currPos, Direction currDir) {
		  //ArrayList<Point> checked = new ArrayList<Point>();
		  //ArrayList<Point> checking = new ArrayList<Point>();
		  
		  // BFS uses Queue data structure
		  Queue queue = new LinkedList();
		  Point[][] pathMap = new Point[this.map.length][this.map.length];
		  int[][] visitedMap = new int[this.map.length][this.map.length];
		  Point powerLocation = new Point(-1,-1);
		  
		  visitedMap[currPos.x][currPos.y] = 2;
		  pathMap[currPos.x][currPos.y] = new Point(-2,-2);
		  
		  Point up = PlayerAI.CellInDir(currPos, currDir);
		  if(!outOfBounds(up)){
			  queue.add(up);
			  visitedMap[up.x][up.y] = 2;
			  pathMap[up.x][up.y] = new Point(currPos.x,currPos.y);
			  if(this.map[up.x][up.y] < 0)
				  return up;
		  }
			  
		  Point left = PlayerAI.CellInDir(currPos, PlayerAI.turnDir(currDir,true));
		  if(!outOfBounds(left)){
			  queue.add(left);
			  visitedMap[left.x][left.y] = 2;
			  pathMap[left.x][left.y] = new Point(currPos.x,currPos.y);
			  if(this.map[left.x][left.y] < 0)
				  return left;
		  }
		  
		  Point right = PlayerAI.CellInDir(currPos, PlayerAI.turnDir(currDir,false));
		  if(!outOfBounds(right)){
			  queue.add(right);
			  visitedMap[right.x][right.y] = 2;
			  pathMap[right.x][right.y] = new Point(currPos.x,currPos.y);
			  if(this.map[right.x][right.y] < 0)
				  return right;
		  }
		  
		  while(!queue.isEmpty()) {
		        Point node = (Point)queue.remove();
		       
		        Point childDown = PlayerAI.CellInDir(node, Direction.DOWN);
		        
		        
		        if(!outOfBounds(childDown) && !isWall(childDown) && visitedMap[childDown.x][childDown.y] == 0){
		        	visitedMap[childDown.x][childDown.y] = 2;
		        	pathMap[childDown.x][childDown.y] = new Point(node.x,node.y);
		        	queue.add(childDown);
		        	
		        	
		        	if(this.map[childDown.x][childDown.y] == -1){
			        	powerLocation = childDown;
			        	break;
			        }
			        
		        }
		        
		        
		        Point childUp = PlayerAI.CellInDir(node, Direction.UP);
		        if(!outOfBounds(childUp) && !isWall(childUp) && visitedMap[childUp.x][childUp.y] == 0){
		        	visitedMap[childUp.x][childUp.y] = 2;
		        	pathMap[childUp.x][childUp.y] = new Point(node.x,node.y);
		        	queue.add(childUp);
		        	 if(this.map[childUp.x][childUp.y] == -1){
				        	powerLocation = childUp;
				        	break;
				        }
		        }
		       
		        Point childLeft = PlayerAI.CellInDir(node, Direction.LEFT);
		     
		        if(!outOfBounds(childLeft) && !isWall(childLeft) && visitedMap[childLeft.x][childLeft.y] == 0){
		        	visitedMap[childLeft.x][childLeft.y] = 2;
		        	pathMap[childLeft.x][childLeft.y] = new Point(node.x,node.y);
		        	queue.add(childLeft);
		        	 if(this.map[childLeft.x][childLeft.y] == -1){
				        	powerLocation = childLeft;
				        	break;
				        }
		        }
		       
		        Point childRight = PlayerAI.CellInDir(node, Direction.RIGHT);
		        if(!outOfBounds(childRight) && !isWall(childRight) && visitedMap[childRight.x][childRight.y] == 0){
		        	visitedMap[childRight.x][childRight.y] = 2;
		        	pathMap[childRight.x][childRight.y] = new Point(node.x,node.y);
		        	queue.add(childRight);
		        	if(this.map[childRight.x][childRight.y] == -1){
			        	powerLocation = childRight;
			        	break;
		        	}
		        }

		  }
		 
		  if(powerLocation.x != -1){
			  Point child = powerLocation;
			  Point parent = pathMap[child.x][child.y];
			  
			  while(pathMap[parent.x][parent.y].x!=-2){
				  child = parent;
				  parent = pathMap[child.x][child.y];
			  }
			  return child;
		  }
		  
		  return powerLocation;
	}
	
	
	
	public boolean CheckAvailableDir(Point pos){
		return(!outOfBounds(pos) && this.map[pos.x][pos.y] <= 0);
	}
	
	public boolean isOccupiedPoint(Point current){
		return this.map[current.x][current.y] != 0;
	}
	
	
	public boolean isOccupiedPointType(Point current,int type ){
		return this.map[current.x][current.y] ==  type;       // true
	}
	
	public boolean isWall(Point current ){
		return (this.map[current.x][current.y])>0;
	}
	
	public int typeOfCellInDir(Point current, Direction currDir){
		//check UP
		if(currDir == Direction.DOWN){
			return typeOfCell(new Point(current.x, current.y+1));
		}
		else if(currDir == Direction.LEFT){
			return typeOfCell(new Point(current.x-1, current.y));
		}
		else if(currDir == Direction.UP){
			return typeOfCell(new Point(current.x, current.y-1));
		}
		else{
			//if RIGHT
			return typeOfCell(new Point(current.x+1, current.y));
		}
	}
	
	public static boolean isTypeWALL(int type){
		return(type > 0);
	}
	
	public boolean isCellInDirWALL(Point current, Direction currDir){
		return isTypeWALL(typeOfCellInDir(current,currDir));
	}
	
}
