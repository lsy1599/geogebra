package geogebra3D.euclidian3D.opengl;

import geogebra.common.awt.GColor;
import geogebra.common.awt.GPoint;
import geogebra.common.kernel.Matrix.CoordMatrix4x4;
import geogebra.common.kernel.Matrix.Coords;
import geogebra.common.kernel.geos.GeoNumeric;
import geogebra.common.main.App;
import geogebra.util.FrameCollector;
import geogebra3D.euclidian3D.Drawable3D;
import geogebra3D.euclidian3D.Drawable3DLists;
import geogebra3D.euclidian3D.EuclidianView3D;
import geogebra3D.euclidian3D.Hits3D;
import geogebra3D.euclidian3D.opengl.RendererJogl.GLlocal;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.glu.GLU;
import javax.media.opengl.glu.GLUtessellator;



/**
 * 
 * Used for openGL display.
 * <p>
 * It provides:
 * <ul>
 * <li> methods for displaying {@link Drawable3D}, with painting parameters </li>
 * <li> methods for picking object </li>
 * </ul>
 * 
 * @author ggb3D
 * 
 */
public abstract class Renderer {

	
	// openGL variables
	protected RendererJogl jogl;
	protected GLU glu = new GLU();
	/** default text scale factor */
	private static final float DEFAULT_TEXT_SCALE_FACTOR = 0.8f;

	

	
	
	/** for polygon tesselation */
	private GLUtessellator tobj;
	
	protected static final int MOUSE_PICK_WIDTH = 3;
	private static final int MOUSE_PICK_DEPTH = 10;
	
	Drawable3D[] drawHits;
	int pickingLoop;
	
	// other
	protected Drawable3DLists drawable3DLists;
	
	protected EuclidianView3D view3D;
	
	// for drawing
	protected CoordMatrix4x4 m_drawingMatrix; //matrix for drawing
	
	
	///////////////////
	//primitives
	//private RendererPrimitives primitives;
	
	///////////////////
	//geometries
	protected Manager geometryManager;
	

	///////////////////
	//textures
	protected Textures textures;

	
	///////////////////
	// arrows
	
	/** no arrows */
	static final public int ARROW_TYPE_NONE=0;
	/** simple arrows */
	static final public int ARROW_TYPE_SIMPLE=1;
	private int m_arrowType=ARROW_TYPE_NONE;
	
	private double m_arrowLength, m_arrowWidth;
	
	
	///////////////////
	// dilation
	
	private static final int DILATION_NONE = 0;
	private static final int DILATION_HIGHLITED = 1;
	private int dilation = DILATION_NONE;
	private double[] dilationValues = {
			1,  // DILATION_NONE
			1.3 // DILATION_HIGHLITED
	};
	
	
	///////////////////
	// for picking
	
	protected GPoint mouse;
	protected boolean waitForPick = false;
	private boolean doPick = false;
	public static final int PICKING_MODE_OBJECTS = 0;
	public static final int PICKING_MODE_LABELS = 1;
	protected int pickingMode = PICKING_MODE_OBJECTS;
	
	/**
	 * creates a renderer linked to an {@link EuclidianView3D} 
	 * @param view the {@link EuclidianView3D} linked to 
	 */
	public Renderer(EuclidianView3D view){
		
        

        //link to 3D view
		this.view3D=view;
		
		//textures
		textures = new Textures(this, view3D.getApplication().getImageManager());	
		
		
	}
	
	/**
	 * 
	 * @return GL instance
	 */
	protected GL getGL(){
		return jogl.getGL();
	}
	
	/**
	 * set GL instance
	 * @param gLDrawable GL drawable
	 */
	public void setGL(GLAutoDrawable gLDrawable){		
		jogl.setGL(gLDrawable);
	}
	
	
	/**
	 * set the list of {@link Drawable3D} to be drawn
	 * @param dl list of {@link Drawable3D}
	 */
	public void setDrawable3DLists(Drawable3DLists dl){
		drawable3DLists = dl;
	}	
	
	/**
	 * re-calc the display immediately
	 */
	abstract public void display();
	
	/** sets if openGL culling is done or not
	 * @param flag
	 */
	public void setCulling(boolean flag){
		if (flag)
			getGL().glEnable(GLlocal.GL_CULL_FACE);
		else
			getGL().glDisable(GLlocal.GL_CULL_FACE);
	}
	
	public void setCullFaceFront(){
		getGL().glCullFace(GLlocal.GL_FRONT); 
	}
	
	public void setCullFaceBack(){
		getGL().glCullFace(GLlocal.GL_BACK); 
	}
	
	/** sets if openGL blending is done or not
	 * @param flag
	 */
	public void setBlending(boolean flag){
		if (flag)
			getGL().glEnable(GLlocal.GL_BLEND);
		else
			getGL().glDisable(GLlocal.GL_BLEND);
	}
	
	protected void drawTransp(){

		setLight(GLlocal.GL_LIGHT1);

		getTextures().loadTextureLinear(Textures.FADING);
		
		getGL().glDisable(GLlocal.GL_CULL_FACE);
		drawable3DLists.drawTransp(this);
		drawable3DLists.drawTranspClosedNotCurved(this);
		
		//TODO fix it
		//getGL().glDisable(GLlocal.GL_TEXTURE_2D);
		//TODO improve this !
		
		
		getGL().glEnable(GLlocal.GL_CULL_FACE);
		getGL().glCullFace(GLlocal.GL_FRONT); 
		drawable3DLists.drawTranspClosedCurved(this);//draws inside parts  
		if (drawable3DLists.containsClippedSurfaces()){
			enableClipPlanesIfNeeded();
			drawable3DLists.drawTranspClipped(this); //clipped surfaces back-faces
			disableClipPlanesIfNeeded();
		}
		getGL().glCullFace(GLlocal.GL_BACK); 
		drawable3DLists.drawTranspClosedCurved(this);//draws outside parts 	
		if (drawable3DLists.containsClippedSurfaces()){
			enableClipPlanesIfNeeded();
			drawable3DLists.drawTranspClipped(this); //clipped surfaces back-faces
			disableClipPlanesIfNeeded();
		}
		

		setLight(GLlocal.GL_LIGHT0);

	}
	
	/**
	 * switch GL_LIGHT0 / GL_LIGHT1
	 * @param light GL_LIGHT0 or GL_LIGHT1
	 */
	abstract protected void setLight(int light);
		
	protected void drawNotTransp(){

        setLight(GLlocal.GL_LIGHT1);

		getTextures().loadTextureLinear(Textures.FADING);

        getGL().glEnable(GLlocal.GL_BLEND);
        
        //getGL().glCullFace(GLlocal.GL_BACK);getGL().glEnable(GLlocal.GL_CULL_FACE);
		getGL().glDisable(GLlocal.GL_CULL_FACE);
        drawable3DLists.drawNotTransparentSurfaces(this);

       		
		//TODO improve this !
		getGL().glEnable(GLlocal.GL_CULL_FACE);
		getGL().glCullFace(GLlocal.GL_FRONT); 
		drawable3DLists.drawNotTransparentSurfacesClosed(this);//draws inside parts  
		if (drawable3DLists.containsClippedSurfaces()){
			enableClipPlanesIfNeeded();
			drawable3DLists.drawNotTransparentSurfacesClipped(this); //clipped surfaces back-faces
			disableClipPlanesIfNeeded();
		}
		getGL().glCullFace(GLlocal.GL_BACK); 
		drawable3DLists.drawNotTransparentSurfacesClosed(this);//draws outside parts 	
		if (drawable3DLists.containsClippedSurfaces()){
			enableClipPlanesIfNeeded();
			drawable3DLists.drawNotTransparentSurfacesClipped(this); //clipped surfaces back-faces
			disableClipPlanesIfNeeded();
		}
		

		setLight(GLlocal.GL_LIGHT0);
	}
	



    /**
     * enable textures
     */
    abstract public void enableTextures();
    
    /**
     * disable multi samples (for antialiasing)
     */
    abstract public void disableTextures();
   
    /**
     * enable  multi samples (for antialiasing)
     */
    final public void enableMultisample(){  	
    	getGL().glEnable(GLlocal.GL_MULTISAMPLE);
    }

    /**
     * disable textures
     */
    final public void disableMultisample(){
    	getGL().glDisable(GLlocal.GL_MULTISAMPLE);
    }
   
    protected void drawFaceToScreen() {
    	//draw face-to screen parts (labels, ...)
        //drawing labels
        //getGL().glEnable(GLlocal.GL_CULL_FACE);
        //getGL().glCullFace(GLlocal.GL_BACK);

        getGL().glEnable(GLlocal.GL_ALPHA_TEST);  //avoid z-buffer writing for transparent parts     
        getGL().glDisable(GLlocal.GL_LIGHTING);
        getGL().glEnable(GLlocal.GL_BLEND);
        enableTextures();
        //getGL().glDisable(GLlocal.GL_BLEND);
        //getGL().glDepthMask(false);
        drawable3DLists.drawLabel(this);
        
        //getGL().glDepthMask(true);
    	//getGL().glAlphaFunc(GLlocal.GL_EQUAL, 1);
        //drawable3DLists.drawLabel(this);
 
        //getGL().glAlphaFunc(GLlocal.GL_GREATER, 0);
        
        
        disableTextures();
        
        if (enableClipPlanes)
        	disableClipPlanes();
        view3D.drawMouseCursor(this);
        if (enableClipPlanes)
    		enableClipPlanes();
        
    }
    
    protected static final int[] GL_CLIP_PLANE = {GLlocal.GL_CLIP_PLANE0, GLlocal.GL_CLIP_PLANE1, GLlocal.GL_CLIP_PLANE2, GLlocal.GL_CLIP_PLANE3, GLlocal.GL_CLIP_PLANE4, GLlocal.GL_CLIP_PLANE5};
    
    protected boolean enableClipPlanes;
    protected boolean waitForUpdateClipPlanes=false;
    
    /**
     * sets if clip planes have to be enabled
     * @param flag flag
     */
    public void setEnableClipPlanes(boolean flag){
    	waitForUpdateClipPlanes = true;
    	enableClipPlanes = flag;
    }
       
    private void enableClipPlane(int n){
    	getGL().glEnable( GL_CLIP_PLANE[n] );   	
    }
    
    private void disableClipPlane(int n){
    	getGL().glDisable( GL_CLIP_PLANE[n] );   	
    }
    
    protected void enableClipPlanes(){
    	for (int n=0; n<6; n++)
    		enableClipPlane(n);
    }
    
    /**
     * enable clipping if needed
     */
    public void enableClipPlanesIfNeeded(){
    	if (!enableClipPlanes)
    		enableClipPlanes();
    }
    
    protected void disableClipPlanes(){
    	for (int n=0; n<6; n++)
    		disableClipPlane(n);
    }
    
    /**
     * disable clipping if needed
     */
    public void disableClipPlanesIfNeeded(){
    	if (!enableClipPlanes)
    		disableClipPlanes();
    }
    
    /**
     * sets the clip plane
     * @param n index of the clip plane
     * @param equation equation of the clip plane
     */
    abstract public void setClipPlane(int n, double[] equation);
    
    /**
     * init drawing matrix to view3D toScreen matrix
     */
    abstract protected void setMatrixView();

    /**
     * reset to projection matrix only
     */
    abstract protected void unsetMatrixView();

    protected void draw(){
    	
        //labels
        drawFaceToScreen();
        
        //init drawing matrix to view3D toScreen matrix
        setMatrixView(); 
 
        setLightPosition();     
        setLight(GLlocal.GL_LIGHT0);

        //drawing the cursor
        //getGL().glEnable(GLlocal.GL_BLEND);
        getGL().glEnable(GLlocal.GL_LIGHTING);
        getGL().glDisable(GLlocal.GL_ALPHA_TEST);       
        getGL().glEnable(GLlocal.GL_CULL_FACE);
        //getGL().glCullFace(GLlocal.GL_FRONT);
        view3D.drawCursor(this);
                 
        //drawWireFrame();
               
        //primitives.enableVBO(gl);
        
        //drawing hidden part
        //getGL().glEnable(GLlocal.GL_CULL_FACE);
        getGL().glEnable(GLlocal.GL_ALPHA_TEST);  //avoid z-buffer writing for transparent parts     
        //getGL().glDisable(GLlocal.GL_BLEND);
        drawable3DLists.drawHiddenNotTextured(this);
        enableTextures();
        //getGL().glColorMask(false,false,false,false); //no writing in color buffer		
        drawable3DLists.drawHiddenTextured(this);
        drawNotTransp();
        //getGL().glColorMask(true,true,true,true);
        disableTextures();
        getGL().glDisable(GLlocal.GL_ALPHA_TEST);       
        
        //getGL().glEnable(GLlocal.GL_BLEND);
        //getGL().glDisable(GLlocal.GL_CULL_FACE);
                
        //drawing transparents parts
        getGL().glDepthMask(false);
        enableTextures();
        drawTransp();      
        getGL().glDepthMask(true);
       
        //drawing labels
        disableTextures();
        getGL().glEnable(GLlocal.GL_CULL_FACE);
        //getGL().glCullFace(GLlocal.GL_BACK);
        //getGL().glEnable(GLlocal.GL_ALPHA_TEST);  //avoid z-buffer writing for transparent parts     
        //getGL().glDisable(GLlocal.GL_LIGHTING);
        getGL().glDisable(GLlocal.GL_BLEND);
        //drawList3D.drawLabel(this);
        //getGL().glEnable(GLlocal.GL_LIGHTING);
        //getGL().glDisable(GLlocal.GL_ALPHA_TEST);              
        
        //drawing hiding parts
        getGL().glColorMask(false,false,false,false); //no writing in color buffer		
        getGL().glCullFace(GLlocal.GL_FRONT); //draws inside parts    
        drawable3DLists.drawClosedSurfacesForHiding(this); //closed surfaces back-faces
        if (drawable3DLists.containsClippedSurfaces()){
        	enableClipPlanesIfNeeded();
        	drawable3DLists.drawClippedSurfacesForHiding(this); //clipped surfaces back-faces
        	disableClipPlanesIfNeeded();
        }
        getGL().glDisable(GLlocal.GL_CULL_FACE);
        drawable3DLists.drawSurfacesForHiding(this); //non closed surfaces
        //getGL().glColorMask(true,true,true,true);
        setColorMask();

        //re-drawing transparents parts for better transparent effect
        //TODO improve it !
        enableTextures();
        getGL().glDepthMask(false);
        getGL().glEnable(GLlocal.GL_BLEND);
        drawTransp();
        getGL().glDepthMask(true);
        disableTextures();
        
        //drawing hiding parts
        getGL().glColorMask(false,false,false,false); //no writing in color buffer		
        getGL().glDisable(GLlocal.GL_BLEND);
        getGL().glEnable(GLlocal.GL_CULL_FACE);
        getGL().glCullFace(GLlocal.GL_BACK); //draws inside parts
        drawable3DLists.drawClosedSurfacesForHiding(this); //closed surfaces front-faces
        if (drawable3DLists.containsClippedSurfaces()){
        	enableClipPlanesIfNeeded();
        	drawable3DLists.drawClippedSurfacesForHiding(this); //clipped surfaces back-faces
        	disableClipPlanesIfNeeded();
        }
        setColorMask();        
        
        //re-drawing transparents parts for better transparent effect
        //TODO improve it !
        enableTextures();
        getGL().glDepthMask(false);
        getGL().glEnable(GLlocal.GL_BLEND);
        drawTransp();
        getGL().glDepthMask(true);
        //getGL().glDisable(GLlocal.GL_TEXTURE_2D);
        
        //drawing not hidden parts
        getGL().glEnable(GLlocal.GL_CULL_FACE);
        //getGL().glDisable(GLlocal.GL_BLEND);
        //getGL().glEnable(GLlocal.GL_TEXTURE_2D);
    	//getGL().glPolygonMode(GLlocal.GL_FRONT, GLlocal.GL_LINE);getGL().glPolygonMode(GLlocal.GL_BACK, GLlocal.GL_LINE);
        drawable3DLists.draw(this);        
        
        //primitives.disableVBO(gl);
            
        //FPS
        getGL().glDisable(GLlocal.GL_LIGHTING);
        getGL().glDisable(GLlocal.GL_DEPTH_TEST);

        //drawWireFrame();       
        
        unsetMatrixView();  
   	
    	//drawFPS();
        
    	getGL().glEnable(GLlocal.GL_DEPTH_TEST);
    	getGL().glEnable(GLlocal.GL_LIGHTING);        	
    }    
     
    /*
    private void drawWireFrame() {
  
    	getGL().glPushAttrib(GLlocal.GL_ALL_ATTRIB_BITS);
    	
    	getGL().glDepthMask(false);
    	getGL().glPolygonMode(GLlocal.GL_FRONT, GLlocal.GL_LINE);getGL().glPolygonMode(GLlocal.GL_BACK, GLlocal.GL_LINE);
        
    	getGL().glLineWidth(5f);
    	
    	getGL().glEnable(GLlocal.GL_LIGHTING);
        getGL().glDisable(GLlocal.GL_LIGHT0);
        getGL().glDisable(GLlocal.GL_CULL_FACE);
        getGL().glDisable(GLlocal.GL_BLEND);
        getGL().glEnable(GLlocal.GL_ALPHA_TEST);
        
    	drawable3DLists.drawTransp(this);
    	drawable3DLists.drawTranspClosedNotCurved(this);
    	drawable3DLists.drawTranspClosedCurved(this);
    	if (drawable3DLists.containsClippedSurfaces()){
    		enableClipPlanesIfNeeded();
    		drawable3DLists.drawTranspClipped(this); 
    		disableClipPlanesIfNeeded();
    	}
    	
    	getGL().glPopAttrib(); 
    }
    */
    
    /**
     * set line width
     * @param width line width
     */
    public void setLineWidth(float width){
    	
		getGL().glLineWidth(width);
		
    }


    //////////////////////////////////////
    // EXPORT IMAGE
    //////////////////////////////////////     
    
    protected boolean needExportImage=false;
    
    /**
     * says that an export image is needed, and call immediate display
     */
    public void needExportImage(){
    	needExportImage = true;
    	display();  	
    }
    
    protected BufferedImage bi;
    
    /**
     * creates an export image (and store it in BufferedImage bi)
     */
    abstract protected void setExportImage();

    /**
     * @return a BufferedImage containing last export image created
     */
    public BufferedImage getExportImage(){
    	App.debug("thumbnail");
    	return bi;
    }   
    
  
      
    
    ///////////////////////////////////////////////////
    //
    // pencil methods
    //
    /////////////////////////////////////////////////////
    
      
    /**
     * sets the material used by the pencil
     * @param color (r,g,b,a) vector
     * 
     */
    abstract public void setColor(Coords color);
    
    abstract public void setColor(geogebra.common.awt.GColor color);
    
    
    //arrows
    
    /**
     * sets the type of arrow used by the pencil.
     * 
     * @param a_arrowType type of arrow, see {@link #ARROW_TYPE_NONE}, {@link #ARROW_TYPE_SIMPLE}, ... 
     */
    public void setArrowType(int a_arrowType){
    	m_arrowType = a_arrowType;
    } 
    
    /**
     * sets the width of the arrows painted by the pencil.
     * 
     * @param a_arrowWidth the width of the arrows
     */
    public void setArrowWidth(double a_arrowWidth){
    	m_arrowWidth = a_arrowWidth;
    } 
    
    
    /**
     * sets the length of the arrows painted by the pencil.
     * 
     * @param a_arrowLength the length of the arrows
     */
    public void setArrowLength(double a_arrowLength){
    	m_arrowLength = a_arrowLength;
    } 
       
    
    //layer
    /**
     * sets the layer to l. Use gl.glPolygonOffset( ).
     * @param l the layer
     */
    public void setLayer(float l){
    	
    	// 0<=l<10
    	// l2-l1>=1 to see something
    	//l=l/3f;
       	getGL().glPolygonOffset(-l*0.05f, -l*10);
    	
       	//getGL().glPolygonOffset(-l*0.75f, -l*0.5f);
       	
       	//getGL().glPolygonOffset(-l, 0);
    }   
    
    //drawing matrix
    
    /**
     * sets the matrix in which coord sys the pencil draws.
     * 
     * @param a_matrix the matrix
     */
    public void setMatrix(CoordMatrix4x4 a_matrix){
    	m_drawingMatrix=a_matrix;
    }
    
    
    /**
     * gets the matrix describing the coord sys used by the pencil.
     * 
     * @return the matrix
     */
    public CoordMatrix4x4 getMatrix(){
    	return m_drawingMatrix;
    }
    
    
    /**
     * sets the drawing matrix to openGLlocal.
     * same as initMatrix(m_drawingMatrix)
     */
    abstract public void initMatrix();
    
    
    
    /**
     * turn off the last drawing matrix set in openGLlocal.
     */
    abstract public void resetMatrix();
       
    
    ///////////////////////////////////////////////////////////
    //drawing geometries
    
    
    final public Manager getGeometryManager(){
    	return geometryManager;
    }
    
    
    ///////////////////////////////////////////////////////////
    //textures
    
    
    public Textures getTextures(){
    	return textures;
    }
   
    
    /** draws a 3D cross cursor
     * @param type 
     */    
    final public void drawCursor(int type){
    	
    	if (!PlotterCursor.isTypeAlready(type))
    		getGL().glDisable(GLlocal.GL_LIGHTING);
    	
    	initMatrix();
    	geometryManager.draw(geometryManager.cursor.getIndex(type));
		resetMatrix();
    	
		if (!PlotterCursor.isTypeAlready(type))
			getGL().glEnable(GLlocal.GL_LIGHTING);
   	
    } 
    
    /**
     * draws a view button
     */
    public void drawViewInFrontOf(){
    	//Application.debug("ici");
    	initMatrix();
    	setBlending(false);
    	geometryManager.draw(geometryManager.getViewInFrontOf().getIndex());
    	setBlending(true);
		resetMatrix();   	
    }
    
    /**
     * draws mouse cursor
     */
    abstract public void drawMouseCursor();
    
    

    
    
    public int startPolygons(){
    	
    	return geometryManager.startPolygons();
    }
    
    /**
     * draw a polygon
     * @param n normal
     * @param v vertices
     */
    public void drawPolygon(Coords n, Coords[] v){
    	geometryManager.drawPolygon(n, v);
    }
    
    public void endPolygons(){
    	geometryManager.endPolygons();
    }
    
    
    
    
     
    /* draws the text s
     * @param x x-coord
     * @param y y-coord
     * @param s text
     * @param colored says if the text has to be colored
     *
    public void drawText(float x, float y, String s, boolean colored){
    	
    	
    	//if (true)    		return;
    	
        getGL().glMatrixMode(GLlocal.GL_TEXTURE);
        getGL().glLoadIdentity();
        
    	getGL().glMatrixMode(GLlocal.GL_MODELVIEW);
    	
    	
    	initMatrix();
    	initMatrix(view3D.getUndoRotationMatrix());
    	

    	textRenderer.begin3DRendering();

    	if (colored)
    		textRenderer.setColor(textColor);
    	
     
        float textScaleFactor = DEFAULT_TEXT_SCALE_FACTOR/((float) view3D.getScale());
    	
    	
        if (x<0)
        	x=x-(s.length()-0.5f)*8; //TODO adapt to police size
        
    	textRenderer.draw3D(s,
                x*textScaleFactor,//w / -2.0f * textScaleFactor,
                y*textScaleFactor,//h / -2.0f * textScaleFactor,
                0,
                textScaleFactor);
    	
        textRenderer.end3DRendering();
     
 
    	
        resetMatrix(); //initMatrix(m_view3D.getUndoRotationMatrix());
    	resetMatrix(); //initMatrix();
    	
    }
    
       
    */
    
        
    
    /////////////////////////
    // FPS
    
    /*
	double displayTime = 0;
	int nbFrame = 0;
	double fps = 0;
    
    private void drawFPS(){
    	
    	if (displayTime==0)
    		displayTime = System.currentTimeMillis();
    	
    	nbFrame++;
    	
    	double newDisplayTime = System.currentTimeMillis();
    	
    	
    	//displayTime = System.currentTimeMillis();
    	if (newDisplayTime > displayTime+1000){
    		
    		

    		fps = 1000*nbFrame/(newDisplayTime - displayTime);
    		displayTime = newDisplayTime;
    		nbFrame = 0;
    	}
    	
    	
    	
    	
        getGL().glMatrixMode(GLlocal.GL_TEXTURE);
        getGL().glLoadIdentity();
        
    	getGL().glMatrixMode(GLlocal.GL_MODELVIEW);
    	
    	getGL().glPushMatrix();
    	getGL().glLoadIdentity();
    	
    	
    	textRenderer.begin3DRendering();

    	
    	textRenderer.setColor(Color.BLACK);
    	  	
        
    	textRenderer.draw3D("FPS="+ ((int) fps),left,bottom,0,1);
    	
        textRenderer.end3DRendering();
        
        getGL().glPopMatrix();
    }
    
       
    */
    
   
    
    //////////////////////////////////////
    // picking
    
    /**
     * sets the mouse locations to (x,y) and asks for picking.
     * 
     * @param x x-coordinate of the mouse
     * @param y y-coordinate of the mouse
     */
    public void setMouseLoc(GPoint p, int pickingMode){
    	mouse = p;
  	
    	this.pickingMode = pickingMode;
    	
    	// on next rending, a picking will be done : see doPick()
    	waitForPick = true;   	
    }
    
    /*
    private boolean intersectionCurvesWaitForPick = false;
    
    public void setIntersectionCurvesWaitForPick(){
    	intersectionCurvesWaitForPick = true;
    }
    */
    
    protected int oldGeoToPickSize;
    protected int geoToPickSize = EuclidianView3D.DRAWABLES_NB;

	protected IntBuffer selectBuffer;
    
	
    public void addOneGeoToPick(){
    	geoToPickSize++;
    }
    

	public void removeOneGeoToPick(){
		geoToPickSize--;
		/*
		if (geoToPickSize<0)
			App.printStacktrace("");
			*/
			
	}
	
	
	
	abstract protected IntBuffer createSelectBufferForPicking(int bufSize);
	
	protected static Drawable3D[] createDrawableListForPicking(int bufSize){
        return new Drawable3D[bufSize];
	}
	
	
	abstract protected void setGLForPicking();
	
	
	abstract protected void pushSceneMatrix();
	
	abstract protected void storePickingInfos(Hits3D hits3D, int pointAndCurvesLoop, int labelLoop);
	
	protected boolean intersectsMouse3D(double zNear, double zFar, double mouseZ){
		//App.debug("\n"+zNear+"\n"+zFar+"\n"+mouseZ+"\n"+view3D.getScreenZOffset());
		return mouseZ - MOUSE_PICK_DEPTH < zNear && mouseZ + MOUSE_PICK_DEPTH > zFar;
		
	}
	
	protected boolean needsNewPickingBuffer = true;
	
    /**
     * does the picking to sets which objects are under the mouse coordinates.
     */
    abstract protected void doPick();

	public enum PickingType {
		POINT_OR_CURVE,
		SURFACE,
		LABEL
	}
	
        
    /**
     * process picking for intersection curves
     * SHOULD NOT BE CALLED OUTSIDE THE DISPLAY LOOP
     */
    abstract public void pickIntersectionCurves();
    
    abstract public void glLoadName(int loop);
    
    public void pick(Drawable3D d, PickingType type){  
    	 pick(d,false,type);
    }
    
    public void pick(Drawable3D d, boolean intersection, PickingType type){  
    	//App.debug(d.getGeoElement()+"\npickingloop="+pickingLoop+"\ndrawHits length="+drawHits.length);  	
    	//Application.debug("1");
    	glLoadName(pickingLoop);//Application.debug("2");
    	Drawable3D ret = d.drawForPicking(this,intersection, type);	
    	//App.debug(pickingLoop+": "+ret);
    	if (ret!=null){
    		//App.debug("---"+ret.getGeoElement());
    		drawHits[pickingLoop] = ret;//Application.debug("4");
    		pickingLoop++;//Application.debug("5");
    	}
    }
    
    public void pickLabel(Drawable3D d){   	
    	glLoadName(pickingLoop);
    	if (d.drawLabelForPicking(this)){
    		//Application.debug(d.getGeoElement());
    		drawHits[pickingLoop] = d;
    		pickingLoop++;
    	}
    }
    
    /** returns the depth between 0 and 2, in double format, from an integer offset 
     *  lowest is depth, nearest is the object
     *  
     *  @param ptr the integer offset
     * */
    protected static float getDepth(int ptr, IntBuffer selectBuffer){
     	
    	return (float) (selectBuffer.get(ptr)& 0xffffffffL) / 0x7fffffff;
    }
    
    public double getScreenZFromPickingDepth(double z){
    	double d = getVisibleDepth()/2;
    	//return (perspNear*(z-(perspFar-perspNear)))/((perspFar-perspNear)*(z-eyeToScreenDistance));
    	
    	if(view3D.getProjection()==EuclidianView3D.PROJECTION_ORTHOGRAPHIC
    			|| view3D.getProjection()==EuclidianView3D.PROJECTION_OBLIQUE){
    		return d*(1-z);
    	}
    	
    	return eyeToScreenDistance*(z-1-d/eyeToScreenDistance)/(z-1-eyeToScreenDistance/d) - view3D.getScreenZOffset();
    }
    

    //////////////////////////////////
    // LIGHTS
    //////////////////////////////////

    
    private float[] light0Position = {1f, 0f, 1f, 0f};
    
    
    protected void setLightPosition(){
    	setLightPosition(GLlocal.GL_LIGHT0, light0Position);
    	setLightPosition(GLlocal.GL_LIGHT1, light0Position);
    }
    
    /**
     * set light position
     * @param light light
     * @param values attribute values
     */
    abstract protected void setLightPosition(int light, float[] values);
  
    /**
     * set light ambiant and diffuse values (white lights)
     * 
     */
    abstract protected void setLightAmbiantDiffuse(float ambiant0, float diffuse0, float ambiant1, float diffuse1);

    //////////////////////////////////
    // clear color
    
    protected boolean waitForUpdateClearColor = false;
    
    public void setWaitForUpdateClearColor(){
    	waitForUpdateClearColor = true;
    }
    
    protected void updateClearColor(){

    	GColor c = view3D.getBackground();
    	float r,g,b;
    	if (view3D.getProjection()==EuclidianView3D.PROJECTION_GLASSES
    			&& !view3D.isPolarized()) { //grayscale for anaglyph glasses
    		r = (float) (c.getGrayScale()/255);
    		g = r;
    		b = r;
    	}else{
    		r = (float) c.getRed()/255;
    		g = view3D.isShutDownGreen() ? 0 : (float) c.getGreen()/255;
    		b = (float) c.getBlue()/255;
    	}
    	
        getGL().glClearColor(r,g,b, 1.0f);   
    }
     
    
    //////////////////////////////////
    // initializations
    
    /**
     * init shaders (when used)
     */
    protected void initShaders(){
    	// no shader here
    }

    /**
     *  Use the shaderProgram that got linked during the init part.
     */
    protected void useShaderProgram(){
    	// no shader here
    }
    
    /**
     * 
     * @return new geometry manager
     */
    abstract protected Manager createManager();
    
 
    
    
    abstract protected void setColorMaterial();

    abstract protected void setLightModel();

	abstract protected void setAlphaFunc();
    
    
    /**
     * ensure that animation is on (needed when undocking/docking 3D view)
     */
    abstract public void resumeAnimator();
    
    //projection mode
    
	int left = 0; int right = 640;
	int bottom = 0; int top = 480;
	
	/** factor for drawing more than between front and back */
	private final static int DEPTH_FACTOR = 2;
		
	public int getLeft(){ return left;	}
	public int getRight(){ return right;	}
	public int getWidth(){return right-left;}
	public int getBottom(){ return bottom;	}
	public int getTop(){ return top;	}
	public int getHeight(){return top - bottom;}
	
	public int getVisibleDepth(){ return getWidth()*2; } //keep visible objects at twice center-to-right distance
	
	
	
	/** for a line described by (o,v), return the min and max parameters to draw the line
	 * @param minmax initial interval
	 * @param o origin of the line
	 * @param v direction of the line
	 * @param extendedDepth says if it looks to real depth bounds, or working depth bounds
	 * @return interval to draw the line
	 */
	public double[] getIntervalInFrustum(double[] minmax, 
			Coords o, Coords v,
			boolean extendedDepth){
			
		double left = (getLeft() - o.get(1))/v.get(1);
		double right = (getRight() - o.get(1))/v.get(1);		
		updateIntervalInFrustum(minmax, left, right);
		
		double top = (getTop() - o.get(2))/v.get(2);
		double bottom = (getBottom() - o.get(2))/v.get(2);
		updateIntervalInFrustum(minmax, top, bottom);
		
		double halfDepth = getVisibleDepth()/2;
		double front = (-halfDepth - o.get(3))/v.get(3);
		double back = (halfDepth - o.get(3))/v.get(3);
		updateIntervalInFrustum(minmax, front, back);
		
		return minmax;
	}
		
	/** return the intersection of intervals [minmax] and [v1,v2]
	 * @param minmax initial interval
	 * @param v1 first value
	 * @param v2 second value
	 * @return intersection interval
	 */
	private static double[] updateIntervalInFrustum(double[] minmax, double v1, double v2){
		
		if (v1>v2){
			double v = v1;
			v1 = v2; v2 = v;
		}
		
		if (v1>minmax[0])
			minmax[0] = v1;
				
		if (v2<minmax[1])
			minmax[1] = v2;
	
		return minmax;
	}
	
	/**
	 * set up the view
	 */
	abstract protected void setView();
	
	protected boolean waitForDisableStencilLines = false;
	
	public void setWaitForDisableStencilLines(){
		waitForDisableStencilLines = true;
	}
	
	protected void disableStencilLines(){
		getGL().glDisable(GLlocal.GL_STENCIL_TEST);
		waitForDisableStencilLines = false;
	}
	
	protected boolean waitForSetStencilLines = false;
	
	public void setWaitForSetStencilLines(){
		waitForSetStencilLines = true;	
	}
	
	
	abstract protected void setStencilLines();
	
	protected void setProjectionMatrixForPicking(){
		
		switch(view3D.getProjection()){
		case EuclidianView3D.PROJECTION_ORTHOGRAPHIC:
			viewOrtho();
			break;
		case EuclidianView3D.PROJECTION_GLASSES:
			viewGlasses();
			break;
		case EuclidianView3D.PROJECTION_PERSPECTIVE:
			viewPersp();
			break;
		case EuclidianView3D.PROJECTION_OBLIQUE:
			viewOblique();
			break;
		}
				
	}
	
	protected void setProjectionMatrix(){
		
		switch(view3D.getProjection()){
		case EuclidianView3D.PROJECTION_ORTHOGRAPHIC:
			viewOrtho();
			break;
		case EuclidianView3D.PROJECTION_PERSPECTIVE:
			viewPersp();
			break;
		case EuclidianView3D.PROJECTION_GLASSES:
			viewGlasses();
			break;
		case EuclidianView3D.PROJECTION_OBLIQUE:
			viewOblique();
			break;
		}
				
		//viewEye();
	}
   
    /**
     * Set Up An Ortho View regarding left, right, bottom, front values
     * 
     */
    abstract protected void viewOrtho();

    
    protected double eyeToScreenDistance = 0;
    //private double distratio;
    
    public void setNear(double val){
    	eyeToScreenDistance = val;
    	updatePerspValues();   	
    }
    
    /** distance camera-near plane */
    private final static double PERSP_NEAR_MIN = 10;
    protected double perspNear = PERSP_NEAR_MIN; 
    protected double perspLeft;
	protected double perspRight;
	protected double perspBottom;
	protected double perspTop;
	protected double perspFar;
	protected double perspDistratio;
	protected double perspFocus;
	protected Coords perspEye;
    
    private void updatePerspValues(){
    	
   	
    	perspNear = eyeToScreenDistance - getVisibleDepth()/2;
    	if (perspNear < PERSP_NEAR_MIN){
    		perspNear = PERSP_NEAR_MIN;
    	}
    	
    	perspFocus = -eyeToScreenDistance + view3D.getScreenZOffset();
    	//App.error(""+ view3D.getScreenZOffset());

    	//ratio so that distance on screen plane are not changed
    	perspDistratio = perspNear/(eyeToScreenDistance);
    	
    	//frustum    	
    	perspLeft = getLeft()*perspDistratio;
    	perspRight = getRight()*perspDistratio;
    	perspBottom = getBottom()*perspDistratio;
    	perspTop = getTop()*perspDistratio;
    	//distance camera-far plane
    	perspFar = perspNear+getVisibleDepth();
    	
    	perspEye = new Coords(0,0,-perspFocus,1);   	
    }
    
    /**
     * 
     * @return coords of the eye (in real coords) when perspective projection
     */
    public Coords getPerspEye(){
    	return perspEye;
    }
    
    /**
     * 
     * @return eyes separation (half of, in real coords)
     */
    public double getEyeSep(){
    	return glassesEyeSep;
    }
    
    abstract protected void viewPersp();
    
    protected double glassesEyeSep;
	protected double glassesEyeSep1;
    
    public void updateGlassesValues(){
    	//half eye separation
    	glassesEyeSep = -view3D.getEyeSep()/2;
    	//eye separation for frustum
    	glassesEyeSep1 = glassesEyeSep*perspDistratio;//(1-distratio);    
    	//Application.debug("eyesep="+eyesep+"\ne1="+e1);
    }
    
    
    abstract protected void viewGlasses();
        
    private static final int EYE_ONE = -1;
    protected static final int EYE_LEFT = 0;
    protected static final int EYE_RIGHT = 1;
    protected int eye = EYE_ONE;
    
    protected void setColorMask(){

    	if (view3D.getProjection()==EuclidianView3D.PROJECTION_GLASSES && !view3D.isPolarized()){
    		if (eye==EYE_LEFT) {
    			getGL().glColorMask(true,false,false,true); //cyan
    			//getGL().glColorMask(false,true,false,true); //magenta
    			//getGL().glColorMask(false,false,false,true);
    		} else {
    			getGL().glColorMask(false,!view3D.isGlassesShutDownGreen(),true,true); //red
    			//getGL().glColorMask(true,false,false,true); //cyan -> green
    			//getGL().glColorMask(false,false,false,true);
    		}
    	} else {
    		getGL().glColorMask(true,true,true,true);
    	}	

    }
    
    enum ExportType { NONE, ANIMATEDGIF, THUMBNAIL_IN_GGBFILE, PNG, CLIPBOARD, UPLOAD_TO_GEOGEBRATUBE };
    
    protected double obliqueX;
	protected double obliqueY;
    private Coords obliqueOrthoDirection; //direction "orthogonal" to the screen (i.e. not visible)
	protected ExportType exportType = ExportType.NONE;
	protected int export_n;
	protected double export_val;
	protected double export_min;
	protected double export_max;
	protected double export_step;
	protected FrameCollector gifEncoder;
	protected int export_i;
	protected GeoNumeric export_num;
    
    public void updateProjectionObliqueValues(){
    	double angle = Math.toRadians(view3D.getProjectionObliqueAngle());
    	obliqueX = -view3D.getProjectionObliqueFactor()*Math.cos(angle);
    	obliqueY = -view3D.getProjectionObliqueFactor()*Math.sin(angle);
    	obliqueOrthoDirection = new Coords(obliqueX, obliqueY, -1, 0);
    }
    
    abstract protected void viewOblique();
    
    public Coords getObliqueOrthoDirection(){
    	return obliqueOrthoDirection;
    }
    
    /**
     * Set Up An Ortho View after setting left, right, bottom, front values
     * @param x left
     * @param y bottom
     * @param w width
     * @param h height
     * 
     */
    protected void setView(int x, int y, int w, int h){
    	left=x-w/2;
    	bottom=y-h/2;
    	right=left+w;
    	top = bottom+h;
    	


    	switch (view3D.getProjection()){
    	case EuclidianView3D.PROJECTION_ORTHOGRAPHIC:
    		break;
    	case EuclidianView3D.PROJECTION_PERSPECTIVE:
    		updatePerspValues();
    		break;
    	case EuclidianView3D.PROJECTION_GLASSES:
    		updatePerspValues();
    		updateGlassesValues();
    		if (view3D.isPolarized()){
    			setWaitForSetStencilLines();
    		}
    		break;
    	}
    	    	
    	setView();
    	
    	view3D.setViewChanged();
    	view3D.setWaitForUpdate();
    }



	public void startAnimatedGIFExport(FrameCollector gifEncoder,
			GeoNumeric num, int n, double val, double min, double max,
			double step) {
		exportType  = ExportType.ANIMATEDGIF;
		
		num.setValue(val);
		num.updateRepaint();
		export_i = 0;

		
		this.export_n = n;
		this.export_num = num;
		this.export_val = val;
		this.export_min = min;
		this.export_max = max;
		this.export_step = step;
		this.gifEncoder = gifEncoder;
		
	}



	public void exportToClipboard() {
		exportType = ExportType.CLIPBOARD;
		
	}

	public void uploadToGeoGebraTube() {
		exportType = ExportType.UPLOAD_TO_GEOGEBRATUBE;
		
	}


	/**
	 * Double.POSITIVE_INFINITY for parallel projections
	 * @return eye to screen distance
	 */
	public double getEyeToScreenDistance() {
		if (view3D.getProjection() == EuclidianView3D.PROJECTION_PERSPECTIVE || view3D.getProjection() == EuclidianView3D.PROJECTION_GLASSES){
			return eyeToScreenDistance;
		}

		return Double.POSITIVE_INFINITY;
	}
	
	
	/**
	 * enable GL textures 2D
	 */
	abstract public void enableTextures2D();
	
	/**
	 * disable GL textures 2D
	 */
	abstract public void disableTextures2D();
	
	/**
	 * generate textures
	 * @param number texture length
	 * @param index indices
	 */
	abstract public void genTextures2D(int number, int[] index);
	
	/**
	 * bind the texture
	 * @param index texture index
	 */
	abstract public void bindTexture(int index);
	
	
	/**
	 * remove a texture
	 * @param index texture index
	 */
	abstract public void removeTexture(int index);
	
	/** 
	 * @param sizeX
	 * @param sizeY
	 * @param buf
	 */
	abstract public void textureImage2D(int sizeX, int sizeY, ByteBuffer buf);
	
	/**
	 * set texture linear parameters
	 */
	abstract public void setTextureLinear();
      
	
	/**
	 * set texture nearest parameters
	 */
	abstract public void setTextureNearest();
	
}
