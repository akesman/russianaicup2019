import model.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static model.WeaponType.ROCKET_LAUNCHER;

public class MyStrategy {

  /***
   * Доработать ходбу
   * Симуляция ходьбы врага
   * Уворот от пуль
   *
   *
   */

  static boolean FINDWEAPON = true;
  static boolean SHOWFIELD = true;
  private static final boolean ATTACK = false;


  boolean initFirst = false;

  enumACTION action;

  int[][] field;
  int[][] fieldCurrent;


  Unit unit; Game game; Debug debug;
  Unit nearestEnemy;


  //
  private boolean HP_LOOT_Exist = true;
  //

  static double distanceSqr(Vec2Double a, Vec2Double b) {
    return (a.getX() - b.getX()) * (a.getX() - b.getX()) + (a.getY() - b.getY()) * (a.getY() - b.getY());
  }

  public UnitAction getAction(Unit unit, Game game, Debug debug) {

    this.game = game;
    this.debug = debug;
    this.unit = unit;
    UnitAction action = new UnitAction();
    nearestEnemy = initNerestEnemy();

    //создаем поле для передвижения
    init();

    moveLogic(action);

    dodgeLogic(action);


    debug.draw(new CustomData.Log("Target action : " + this.action));

    /*if(unit.getWeapon()!=null) {
      action.setSwapWeapon(true);
    }
    else {*/
      action.setSwapWeapon(false);
    //}

    Vec2Double aim = new Vec2Double(0, 0);
    if (nearestEnemy != null) {
      aim = new Vec2Double(nearestEnemy.getPosition().getX() - unit.getPosition().getX(),
              nearestEnemy.getPosition().getY() - unit.getPosition().getY());
    }
    action.setAim(aim);

    shotLogic(action);

    simulationWorld(action, 100);

    action.setPlantMine(false);
    return action;
  }

  private void init() {
    if(!initFirst) {
      genField();
      initFirst = true;
      fieldCurrent = new int[field.length][field.length];
    }
  }

  private void dodgeLogic(UnitAction action) {

    Bullet minBullet = null;

    minBullet = getMinBullet();

    if(minBullet!=null) {

      simulationBullet(minBullet);

        debug.draw(new CustomData.Log("shot  : " + "dodge"));

        if(minBullet.getPosition().getY() < unit.getPosition().getY() + unit.getSize().getY())
          action.setJump(true);

        if (minBullet.getPosition().getX() > (unit.getPosition().getX() - unit.getSize().getX()/2))
          action.setVelocity(40);
        else
          action.setVelocity(-40);



    }
  }

  public void simulationWorld(UnitAction action, double timeAction)
  {
    double velocity = action.getVelocity();
    double timeJump = 0;
    Vector2D vector2D = new Vector2D(unit.getPosition());

    timeJump = unit.getJumpState().getMaxTime();

    double time_tik = 1/game.getProperties().getTicksPerSecond();

    for(double i=0; i<timeAction/game.getProperties().getTicksPerSecond();i+=time_tik)
    {
      double bufferX = vector2D.x + velocity*time_tik;
      double bufferY = vector2D.y;

      if(timeJump > 0 && action.isJump()) {
        bufferY += unit.getJumpState().getSpeed() * time_tik;
        timeJump-= time_tik;
      }
      else {
          if(!unit.isOnGround()) bufferY += -10 * time_tik;
      }

      if((int)bufferX<game.getLevel().getTiles().length && (int)bufferX>0)
        if(game.getLevel().getTiles()[(int)bufferX][(int)vector2D.y] == Tile.WALL)
        {

        }
        else {
          vector2D.x = bufferX;
          vector2D.y = bufferY;
        }

                //draw
      drawRect((float)vector2D.x, (float)vector2D.y, 0.1f , enumColorDraw.RED.colorFloat);
    }
  }

  private Bullet getMinBullet() {
    double minDis = 999;
    Bullet minBullet = null;
    //смотрим ближайшую пулю
    for(Bullet bullet: game.getBullets())
    {
      if(bullet.getPlayerId() == unit.getPlayerId()) continue;
      double a = getDistance(bullet.getPosition() , unit.getPosition());
      if(a < minDis)
      {
        minDis = a;
        minBullet = bullet;
      }
    }
    return minBullet;
  }

  private boolean simulationBullet(Bullet bullet) {

    Vector2D vector2D = new Vector2D(bullet.getPosition());
    Vector2D velocity = new Vector2D(bullet.getVelocity());

    velocity.normalize();

    for(double i=0;i<10;i+=0.2)
    {

      double x = vector2D.x + velocity.x * i;
      double y = vector2D.y + velocity.y * i;

      if(game.getLevel().getTiles()[(int)x][(int)y] == Tile.WALL) return false;

      if(x>unit.getPosition().getX() && x<(unit.getPosition().getX()+ unit.getSize().getX() )
      && y>unit.getPosition().getY() && y<(unit.getPosition().getY() + unit.getSize().getY()) ) return true;

      drawRect((float)x, (float)y,0.4f,enumColorDraw.RED.colorFloat);
    }

    return false;
  }

  private void shotLogic(UnitAction action) {

    Vector2D shotVector = simulationShot();

    if(unit.getWeapon()!=null && shotVector!=null) {

      boolean check = false;
      if(unit.getWeapon()!=null) {
        double spread = unit.getWeapon().getSpread();
        double angle = unit.getWeapon().getLastAngle();
        if(simulateVByRotate(new Vector2D(unit.getPosition()), angle+spread) ||
        simulateVByRotate(new Vector2D(unit.getPosition()), angle-spread))
          check = true;
      }

      if(this.action != enumACTION.findHp && check != true) {
        //action.setJump(false);
        action.setVelocity(0);
      }
    }

    if(shotVector!= null)// && minSpread)
    {
      Vec2Double aim = new Vec2Double(shotVector.x - unit.getPosition().getX(),
              shotVector.y - unit.getPosition().getY() - unit.getSize().getY()/2);
      action.setAim(aim);
      if(ATTACK) action.setShoot(true);
      drawLine(new Vec2Double(unit.getPosition().getX(),(unit.getPosition().getY()+ (game.getProperties().getUnitSize().getY()/2))),
             new Vec2Double(shotVector.x,shotVector.y) ,  enumColorDraw.WHITE.colorFloat);


    }
    else
      action.setShoot(false);

  }

  //симулируем верхнюю и нижнюю границу выстрела
  //возвращаем если попали в стену
  private boolean simulateVByRotate(Vector2D vector2D, double angle) {
    Vector2D vectorX = new Vector2D(100,0);

    vectorX.rotateBy(angle);

    vectorX.normalize();

    for (double i = 0; i < 1; i+=0.1) {
      double x   = vector2D.x + i*(vectorX.x );
      double y   = vector2D.y + unit.getSize().getY()/2 + i*(vectorX.y);
      drawRect((float)(  x), (float) (  y), 0.2f, enumColorDraw.BLUE.colorFloat);

      if(game.getLevel().getTiles()[(int)x][(int)y] == Tile.WALL) return true;

    }

    return false;
  }

  private Vector2D simulationShot() {
    Vector2D vector2Result = null;

    for(int i = 2 ;i< 10; i+=2) {
      Vector2D vector2D = new Vector2D(unit.getPosition());
      Vector2D vector2D_1 = new Vector2D(nearestEnemy.getPosition().getX(), nearestEnemy.getPosition().getY());
      // истиную точку вылета пули
      vector2D.add(0, game.getProperties().getUnitSize().getY()/2);

      vector2D_1.add(0, game.getProperties().getUnitSize().getY()/i);

      vector2D_1.subtract(vector2D);
      vector2D_1.normalize();

      for (double time = 0; time < 40; time += 0.2f) {

        double x = vector2D.x + vector2D_1.x * time;
        double y = vector2D.y + vector2D_1.y * time;
        drawRect((float) x, (float) y, 0.2f, enumColorDraw.RED.colorFloat);

        if(game.getLevel().getTiles()[(int)x][(int)y] == Tile.WALL) {
          vector2Result = null;
          break;
        }

        if(y>nearestEnemy.getPosition().getY() &&
                y<(nearestEnemy.getPosition().getY()+game.getProperties().getUnitSize().getY()) &&
                x>(nearestEnemy.getPosition().getX()-game.getProperties().getUnitSize().getX()/2) &&
                x<(nearestEnemy.getPosition().getX()+game.getProperties().getUnitSize().getX()/2)) {
          vector2Result = new Vector2D(x,y);
          return  vector2Result;
        }
      }

      if(vector2Result != null) return vector2Result;
    }
    return vector2Result;
  }

  private void moveLogic(UnitAction action) {

    //копируем карту
    copyArrayField();

    //наносим на нее текущее положение врага
    enemyUpdate();

    //обновляем текущее действие
    updateAction();

    //отностиельно действия выбираем точку
    Vec2Double targetPos = findMinCurrentTarget();

    if(targetPos!=null) {
      //идем к точке
      move(action, targetPos);
    }
  }

  private Vec2Double findMinCurrentTarget() {
    Vec2Double targetPos = null;
    targetPos = getCurrenttarget(this.action);
    return targetPos;
  }

  private void copyArrayField() {
    for(int i=0;i<field.length;i++)
      for(int j = 0;j<field[i].length;j++)
      {
        fieldCurrent[i][j] = field[i][j];
      }
  }

  private Vec2Double getCurrenttarget(enumACTION action) {
    switch (action)
    {
      case findWeapon:
        return funFindWeapon();
      case findHp:
        return funFindHp();
      case deffance:
        break;
      case attack:
        return funAttack();
    }

    return null;
  }

  private Vec2Double funAttack() {
    Vec2Double vec2Double = nearestEnemy.getPosition();
    return vec2Double;
  }

  private Vec2Double funFindHp() {
    LootBox nearestHP = null;
    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.HealthPack) {
        if (nearestHP == null || distanceSqr(unit.getPosition(),
                lootBox.getPosition()) < distanceSqr(unit.getPosition(), nearestHP.getPosition())) {
          nearestHP = lootBox;
        }
      }
    }

    if(nearestHP == null) {
      HP_LOOT_Exist = false;
      return null;
    }

    return  nearestHP.getPosition();
  }

  private Vec2Double funFindWeapon() {
    LootBox nearestWeapon = null;

    for (LootBox lootBox : game.getLootBoxes()) {
      if (lootBox.getItem() instanceof Item.Weapon) {
        Item.Weapon weapon = (Item.Weapon) lootBox.getItem();
        if (nearestWeapon == null || getDistance(unit.getPosition(),
                lootBox.getPosition()) < getDistance(unit.getPosition(), nearestWeapon.getPosition()) &&
                weapon.getWeaponType() != ROCKET_LAUNCHER) {
          nearestWeapon = lootBox;
        }
      }
    }

    Vec2Double weaponPosition = nearestWeapon.getPosition();

    return weaponPosition;
  }

  private void updateAction() {
    action = enumACTION.attack;
    if((unit.getWeapon()== null  ) && FINDWEAPON)  action = enumACTION.findWeapon;
    else if(unit.getHealth() <= 75 && HP_LOOT_Exist) action = enumACTION.findHp;
    else action = enumACTION.attack;
  }

  private void enemyUpdate() {
    int posEnemyX = (int)nearestEnemy.getPosition().getX();
    int posEnemyY = (int) ((int)nearestEnemy.getPosition().getY() + nearestEnemy.getSize().getY()/2);

    fieldCurrent[posEnemyY][posEnemyX] = 100;
    if((posEnemyY+1)<game.getLevel().getTiles()[posEnemyY].length && game.getLevel().getTiles()[posEnemyX][posEnemyY+1] == Tile.EMPTY) fieldCurrent[posEnemyY+1][posEnemyX] = 100;
    if((posEnemyY-1)<game.getLevel().getTiles()[posEnemyY].length && game.getLevel().getTiles()[posEnemyX][posEnemyY-1] == Tile.EMPTY) fieldCurrent[posEnemyY-1][posEnemyX] = 100;
    if((posEnemyX+1)<game.getLevel().getTiles()[posEnemyX].length && game.getLevel().getTiles()[posEnemyX+1][posEnemyY] == Tile.EMPTY) fieldCurrent[posEnemyY][posEnemyX+1] = 100;
    if((posEnemyX-1)<game.getLevel().getTiles()[posEnemyX].length && game.getLevel().getTiles()[posEnemyX-1][posEnemyY] == Tile.EMPTY) fieldCurrent[posEnemyY][posEnemyX-1] = 100;
  }


  private Unit initNerestEnemy() {
    Unit nearestEnemy = null;
    for (Unit other : game.getUnits()) {
      if (other.getPlayerId() != unit.getPlayerId()) {
        if (nearestEnemy == null || distanceSqr(unit.getPosition(),
                other.getPosition()) < distanceSqr(unit.getPosition(), nearestEnemy.getPosition())) {
          nearestEnemy = other;
        }
      }
    }
    return nearestEnemy;
  }

  private void move(UnitAction action, Vec2Double targetPos) {

    ArrayList<Vec2Float> track;

    //ищем весь путь
    track = A_Start_Track(targetPos);

    //ищем ближайшую точку
    Vec2Float targetPosMinDist =null;

    if(track.size()>1)
      targetPosMinDist =track.get(1);
    else if(track.size()>0)
      targetPosMinDist =track.get(0);

    if(targetPosMinDist!=null) {

      targetPosMinDist.setX((float) (targetPosMinDist.getX() + unit.getSize().getX()/2));

      drawRect(targetPosMinDist.getX(), targetPosMinDist.getY(), 0.3f, enumColorDraw.BLUE.getColorFloat());

      double speed = game.getProperties().getUnitMaxHorizontalSpeed();

      if (targetPosMinDist.getX() > (unit.getPosition().getX() - unit.getPosition().getX()/2))


        action.setVelocity(speed);
     // else if(targetPosMinDist.getX() >= unit.getPosition().getX() && targetPosMinDist.getX() <= unit.getPosition().getX() )

      // action.setVelocity(0);
      else
        action.setVelocity(-speed);



      if (targetPosMinDist.getY()   > unit.getPosition().getY()) {
        action.setJump(true);
        action.setJumpDown(false);
      } else {
        action.setJump(false);
        action.setJumpDown(true);
      }
    }
  }



  private void genField() {
    int size = game.getLevel().getTiles().length;



    field = new int[size][size];
    for(int i=0;i<size;i++) {
      for (int j = 0; j < game.getLevel().getTiles()[i].length; j++) {
        switch (game.getLevel().getTiles()[i][j]) {
          case EMPTY:
            field[j][i] = 3;
            break;
        }
      }

      for (int j = 0; j < game.getLevel().getTiles()[i].length; j++) {
        switch (game.getLevel().getTiles()[i][j]) {
          case WALL:
            field[j][i] = 100;
            if ((j + 1) < game.getLevel().getTiles()[i].length && game.getLevel().getTiles()[i][j + 1] == Tile.EMPTY) {
              field[j + 1][i] = 0;
            }
            break;
          case PLATFORM:
            //field[j][i] = 20;
            break;
          case LADDER:
            //field[j][i]= 10;
            break;
          case JUMP_PAD:
            field[j][i] = 100;
            if ((j + 1) < game.getLevel().getTiles()[i].length && game.getLevel().getTiles()[i][j + 1] == Tile.EMPTY)
              field[j + 1][i] = 100;
            if ((i + 1) < game.getLevel().getTiles()[i].length && game.getLevel().getTiles()[i + 1][j] == Tile.EMPTY)
              field[j][i + 1] = 100;
            //if((i-1)>=0 && game.getLevel().getTiles()[i-1][j] == Tile.EMPTY) field[j][i-1] = 100;
            break;
        }
      }
    }
  }

  private ArrayList<Vec2Float> A_Start_Track(Vec2Double targetPos) {
    ArrayList<Vec2Float> track;

    AStar as = new AStar(fieldCurrent, (int)unit.getPosition().getX(), (int)unit.getPosition().getY(), false);
    List<AStar.Node> path = as.findPathTo((int)targetPos.getX(), (int)targetPos.getY());
    track = new ArrayList<>();
    if (path != null) {
      path.forEach((n) -> {

        track.add(new Vec2Float((float)(n.x),n.y));
        //field[n.y][n.x] = 1;
        drawRect((float)(n.x),n.y,0.2f, enumColorDraw.WHITE.getColorFloat());

      });
    }

    if(SHOWFIELD)
      for(int i=0;i<fieldCurrent.length;i++)
      {
        for(int j = 0;j<fieldCurrent[i].length;j++)
        {
          //if(field[i][j] == 100)
            drawLabel("" +fieldCurrent[i][j], j,i,0.5f, enumColorDraw.RED.getColorFloat());
        }
      }

    return track;
  }

  class AStar {
    private final List<Node> open;
    private final List<Node> closed;
    private final List<Node> path;
    private final int[][] maze;
    private Node now;
    private final int xstart;
    private final int ystart;
    private int xend, yend;
    private final boolean diag;

    // Node class for convienience
     class Node implements Comparable {
      public Node parent;
      public int x, y;
      public double g;
      public double h;
      Node(Node parent, int xpos, int ypos, double g, double h) {
        this.parent = parent;
        this.x = xpos;
        this.y = ypos;
        this.g = g;
        this.h = h;
      }
      // Compare by f value (g + h)
      @Override
      public int compareTo(Object o) {
        Node that = (Node) o;
        return (int)((this.g + this.h) - (that.g + that.h));
      }
    }

    AStar(int[][] maze, int xstart, int ystart, boolean diag) {
      this.open = new ArrayList<>();
      this.closed = new ArrayList<>();
      this.path = new ArrayList<>();
      this.maze = maze;
      this.now = new Node(null, xstart, ystart, 0, 0);
      this.xstart = xstart;
      this.ystart = ystart;
      this.diag = diag;
    }
    /*
     ** Finds path to xend/yend or returns null
     **
     ** @param (int) xend coordinates of the target position
     ** @param (int) yend
     ** @return (List<Node> | null) the path
     */
    public List<Node> findPathTo(int xend, int yend) {
      this.xend = xend;
      this.yend = yend;
      this.closed.add(this.now);
      addNeigborsToOpenList();
      while (this.now.x != this.xend || this.now.y != this.yend) {
        if (this.open.isEmpty()) { // Nothing to examine
          return null;
        }
        this.now = this.open.get(0); // get first node (lowest f score)
        this.open.remove(0); // remove it
        this.closed.add(this.now); // and add to the closed
        addNeigborsToOpenList();
      }
      this.path.add(0, this.now);
      while (this.now.x != this.xstart || this.now.y != this.ystart) {
        this.now = this.now.parent;
        this.path.add(0, this.now);
      }
      return this.path;
    }
    /*
     ** Looks in a given List<> for a node
     **
     ** @return (bool) NeightborInListFound
     */
    private  boolean findNeighborInList(List<Node> array, Node node) {
      return array.stream().anyMatch((n) -> (n.x == node.x && n.y == node.y));
    }
    /*
     ** Calulate distance between this.now and xend/yend
     **
     ** @return (int) distance
     */
    private double distance(int dx, int dy) {
      if (this.diag) { // if diagonal movement is alloweed
        return this.now.x+this.now.y;//Math.multiplyExact(this.now.x + dx - this.xend, this.now.y + dy - this.yend);
        //return Math.multiplyExact(this.now.x + dx - this.xend, this.now.y + dy - this.yend); // return hypothenuse
      } else {
        return Math.abs(this.now.x + dx - this.xend) + Math.abs(this.now.y + dy - this.yend); // else return "Manhattan distance"
      }
    }
    private void addNeigborsToOpenList() {
      Node node;
      for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
          if (!this.diag && x != 0 && y != 0) {
            continue; // skip if diagonal movement is not allowed
          }
          node = new Node(this.now, this.now.x + x, this.now.y + y, this.now.g, this.distance(x, y));
          if ((x != 0 || y != 0) // not this.now
                  && this.now.x + x >= 0 && this.now.x + x < this.maze[0].length // check maze boundaries
                  && this.now.y + y >= 0 && this.now.y + y < this.maze.length
                  && this.maze[this.now.y + y][this.now.x + x] != -1 // check if square is walkable
                  && !findNeighborInList(this.open, node) && !findNeighborInList(this.closed, node)) { // if not already done
            node.g = node.parent.g + 1.; // Horizontal/vertical cost = 1.0
            node.g += maze[this.now.y + y][this.now.x + x]; // add movement cost for this square

            // diagonal cost = sqrt(hor_cost² + vert_cost²)
            // in this example the cost would be 12.2 instead of 11
                        /*
                        if (diag && x != 0 && y != 0) {
                            node.g += .4;	// Diagonal movement cost = 1.4
                        }
                        */
            this.open.add(node);
          }
        }
      }
      try {
        Collections.sort(this.open);
      }catch (Exception e){}
    }
  }

  //DRAW

  public void drawLine(Vec2Double v1,  Vec2Double v2, ColorFloat colorFloat)
  {debug.draw(new CustomData.Line(new Vec2Float((float)v1.getX(), (float)v1.getY()), new Vec2Float((float)v2.getX(), (float)v2.getY()), 0.1f, colorFloat)); }

  public void drawRect(float x, float y, float size, ColorFloat colorFloat)
  {debug.draw(new CustomData.Rect(new Vec2Float(x, y), new Vec2Float(size, size), colorFloat)); }

  public void drawLabel(String text, float x, float y, float size, ColorFloat colorFloat)
  { debug.draw(new CustomData.PlacedText(text, new Vec2Float(x, y),TextAlignment.CENTER, 14, colorFloat)); }

  enum enumColorDraw
  {
     RED(new ColorFloat(1,0,0,0.7f)),
     BLUE(new ColorFloat(0,0,1,1)),
     WHITE(new ColorFloat(1,1,1,1));
     private ColorFloat colorFloat;

    enumColorDraw(ColorFloat colorFloat) {
      this.colorFloat = colorFloat;
    }

    public ColorFloat getColorFloat() {
      return colorFloat;
    }
  }

  enum enumACTION
  {
    findWeapon,
    findHp,
    deffance,
    attack
  }


  public  double getDistance(Vec2Float v1, Vec2Double v2)
  {
    return Math.hypot(v1.getX()-v2.getX(), v1.getY()-v2.getY());
  }

  public  double getDistance(Vec2Float v1, Vec2Float v2)
  {
    return Math.hypot(v1.getX()-v2.getX(), v1.getY()-v2.getY());
  }

  public  double getDistance(Vec2Double v1, Vec2Double v2)
  {
    return Math.hypot(v1.getX()-v2.getX(), v1.getY()-v2.getY());
  }

  static public class Vector2D{

    public double x;
    public double y;

    public Vector2D() { }

    public Vector2D(double x, double y) {
      this.x = x;
      this.y = y;
    }

    public Vector2D(Vector2D f) {
      this.x = f.x;
      this.y = f.y;
    }

    public Vector2D(Vec2Float f) {
      this.x = f.getX();
      this.y = f.getY();
    }

    public Vector2D(Vec2Double position) {
      this.x = position.getX();
      this.y = position.getY();
    }

    public double getLength() {
      return Math.sqrt(x * x + y * y);
    }


    public double distance(double vx, double vy) {
      vx -= x;
      vy -= y;
      return Math.sqrt(vx * vx + vy * vy);
    }

    public double distance(Vec2Double v) {
      double vx = v.getX() - this.x;
      double vy = v.getY() - this.y;
      return Math.sqrt(vx * vx + vy * vy);
    }

    public double distance(Vector2D v) {
      double vx = v.x - this.x;
      double vy = v.y - this.y;
      return Math.sqrt(vx * vx + vy * vy);
    }

    public double getAngle() {
      return Math.atan2(y, x);
    }

    public void normalize() {
      double magnitude = getLength();
      x /= magnitude;
      y /= magnitude;
    }

    public static Vector2D normalize(Vector2D v1) {
      Vector2D d = new Vector2D(v1);
      float l = (float) (1 / Math.sqrt(d.x*d.x + d.y*d.y));
      d.x *= l;
      d.y *= l;
      return d;
    }

    public void add(Vector2D v) {
      this.x += v.x;
      this.y += v.y;
    }

    public void add(Vec2Double v) {
      this.x += v.getX();
      this.y += v.getY();
    }

    public void add(double vx, double vy) {
      this.x += vx;
      this.y += vy;
    }

    public Vector2D getAdded(Vector2D v) {
      return new Vector2D(this.x + v.x, this.y + v.y);
    }

    public static Vector2D subtract(Vector2D v, Vector2D c) {
      return new Vector2D(v.x -= c.x,v.y -= c.y);
    }

    public void subtract(Vector2D v) {
      this.x -= v.x;
      this.y -= v.y;
    }

    public void subtract(Vec2Double v) {
      this.x -= v.getX();
      this.y -= v.getY();
    }

    public void subtract(double vx, double vy) {
      this.x -= vx;
      this.y -= vy;
    }

    public static Vector2D add(Vector2D v , Vector2D o) {
      return new Vector2D(o.x + v.x, o.y + v.y);
    }

    public static Vector2D mul(Vector2D v , Vector2D o) {
      return new Vector2D(o.x * v.x, o.y * v.y);
    }

    public static Vector2D mul(Vector2D v , double o) {
      return new Vector2D(o * v.x, o * v.y);
    }

    public Vector2D getSubtracted(Vector2D v) {
      return new Vector2D(this.x - v.x, this.y - v.y);
    }

    public void multiply(double scalar) {
      x *= scalar;
      y *= scalar;
    }

    public Vector2D getMultiplied(double scalar) {
      return new Vector2D(x * scalar, y * scalar);
    }

    public void divide(double scalar) {
      x /= scalar;
      y /= scalar;
    }

    public Vector2D getDivided(double scalar) {
      return new Vector2D(x / scalar, y / scalar);
    }

    static public Vector2D divided(Vector2D v ,double scalar) {
      return new Vector2D(v.x / scalar, v.y / scalar);
    }

    public Vector2D getPerp() {
      return new Vector2D(-y, x);
    }

    public double dot(Vector2D v) {
      return (this.x * v.x + this.y * v.y);
    }

    public double dot(double vx, double vy) {
      return (this.x * vx + this.y * vy);
    }

    public static double dot(Vector2D v1, Vector2D v2) {
      return v1.x * v2.x + v1.y * v2.y;
    }

    public double cross(Vector2D v) {
      return (this.x * v.y - this.y * v.x);
    }

    public double cross(double vx, double vy) {
      return (this.x * vy - this.y * vx);
    }

    public static double cross(Vector2D v1, Vector2D v2) {
      return (v1.x * v2.y - v1.y * v2.x);
    }

    public double project(Vector2D v) {
      return (this.dot(v) / this.getLength());
    }

    public double project(double vx, double vy) {
      return (this.dot(vx, vy) / this.getLength());
    }

    public static double project(Vector2D v1, Vector2D v2) {
      return (dot(v1, v2) / v1.getLength());
    }

    public void rotateBy(double angle) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      double rx = x * cos - y * sin;
      y = x * sin + y * cos;
      x = rx;
    }

    public Vector2D getRotatedBy(double angle) {
      double cos = Math.cos(angle);
      double sin = Math.sin(angle);
      return new Vector2D(x * cos - y * sin, x * sin + y * cos);
    }

    public void reverse() {
      x = -x;
      y = -y;
    }

    public Vector2D getReversed() {
      return new Vector2D(-x, -y);
    }
  }

}