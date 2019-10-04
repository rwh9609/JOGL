package JOGL;


import graphicslib3D.*;
import graphicslib3D.GLSLUtils.*;

import java.nio.*;
import javax.swing.*;

import static com.jogamp.opengl.GL.GL_ARRAY_BUFFER;
import static com.jogamp.opengl.GL.GL_STATIC_DRAW;
import static com.jogamp.opengl.GL4.*;
import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.common.nio.Buffers;
import java.util.Random;

public class Snowflake extends JFrame implements GLEventListener {
	
	private GLCanvas myCanvas;
	private int rendering_program;
	private int vao[] = new int[1];
	private int vbo[] = new int[2];
	private float cameraX, cameraY, cameraZ;
	private GLSLUtils util = new GLSLUtils();
	static private int N=5; //Recursion level
	private float[] vertex_positions=new float[3*2];//Three points, two coordinates
	
	public Snowflake() {	
		setTitle("Koch Snowflake");
		setSize(600, 600);
		//Making sure we get a GL4 context for the canvas
        	GLProfile profile = GLProfile.get(GLProfile.GL4);
        	GLCapabilities capabilities = new GLCapabilities(profile);
		myCanvas = new GLCanvas(capabilities);
 		//end GL4 context
		myCanvas.addGLEventListener(this);
		getContentPane().add(myCanvas);
		this.setVisible(true);
	}

	public void display(GLAutoDrawable drawable) {	
		GL4 gl = (GL4) GLContext.getCurrentGL();
		//Define the triangle
		float[] v1=new float[2];//Two coordinates
		float[] v2=new float[2];//Two coordinates
		float[] v3=new float[2];//Two coordinates
		//The first three vertices define the starting triangle
		//Equilateral triangle centered at the origin
		float side_length=1.0f;
		//Top vertex - x and y
		v1[0]=0; v1[1]=side_length*(float)Math.sqrt(3)/3;
		//Bottom left
		v2[0]=-0.5f*side_length; v2[1]=-(float)Math.sqrt(3)*side_length/6;
		//Bottom right
		v3[0]=0.5f*side_length; v3[1]=-(float)Math.sqrt(3)*side_length/6;
		//Done defining triangle
		
		gl.glClear(GL_DEPTH_BUFFER_BIT);

		gl.glUseProgram(rendering_program);

		int mv_loc = gl.glGetUniformLocation(rendering_program, "mv_matrix");
		int proj_loc = gl.glGetUniformLocation(rendering_program, "proj_matrix");

		float aspect = (float) myCanvas.getWidth() / (float) myCanvas.getHeight();
		Matrix3D pMat = orthogonal(-1.5f,1.5f,1.5f,-1.5f,0.1f,1000.0f);

		Matrix3D vMat = new Matrix3D();
		vMat.translate(-cameraX, -cameraY, -cameraZ);
		//Just drawing 2D - not moving the object
		Matrix3D mMat = new Matrix3D();
		mMat.setToIdentity();

		Matrix3D mvMat = new Matrix3D();
		mvMat.concatenate(vMat);
		mvMat.concatenate(mMat);

		gl.glUniformMatrix4fv(mv_loc, 1, false, mvMat.getFloatValues(), 0);
		gl.glUniformMatrix4fv(proj_loc, 1, false, pMat.getFloatValues(), 0);

		//gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);
		gl.glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);//We are only passing two components
		gl.glEnableVertexAttribArray(0);

		gl.glEnable(GL_DEPTH_TEST);
		gl.glDepthFunc(GL_LEQUAL);

		processLine(v1, v2, N);
		processLine(v2, v3, N);
		processLine(v3, v1, N);


	}

	public void init(GLAutoDrawable drawable) {	
		GL4 gl = (GL4) drawable.getGL();
		rendering_program = createShaderProgram();
		cameraX = 0.0f; cameraY = 0.0f; cameraZ = 3.0f;
		gl.glGenVertexArrays(vao.length, vao, 0);
		gl.glBindVertexArray(vao[0]);
		gl.glGenBuffers(vbo.length, vbo, 0);

		gl.glBindBuffer(GL_ARRAY_BUFFER, vbo[0]);

	}


	private Matrix3D perspective(float fovy, float aspect, float n, float f) {	
		float q = 1.0f / ((float) Math.tan(Math.toRadians(0.5f * fovy)));
		float A = q / aspect;
		float B = (n + f) / (n - f);
		float C = (2.0f * n * f) / (n - f);
		Matrix3D r = new Matrix3D();
		r.setElementAt(0,0,A);
		r.setElementAt(1,1,q);
		r.setElementAt(2,2,B);
		r.setElementAt(3,2,-1.0f);
		r.setElementAt(2,3,C);
		r.setElementAt(3,3,0.0f);
		return r;
	}

	private Matrix3D orthogonal(float left, float right, float top, float bottom, float near, float far) {

		Matrix3D r = new Matrix3D();
		r.setElementAt(0,0,2.0/(right-left));
		r.setElementAt(1,1,2.0/(top-bottom));
		r.setElementAt(2,2,1/(far-near));
		r.setElementAt(3,3,1.0f);
		r.setElementAt(0,3,-(right+left)/(right-left));
		r.setElementAt(1,3,-(top+bottom)/(top-bottom));
		r.setElementAt(2, 3, -near/(far-near));
		return r;
	}

	public static void main(String[] args) { 
		
		new Snowflake();
		System.out.println("Depth of recusion: " + N);
		System.out.println("Number of sides: " + numberOfSides);
		System.out.println("Perimeter: " + perimeter);
		System.out.println("Area: " + area);

	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {}
	public void dispose(GLAutoDrawable drawable) {}

	private int createShaderProgram() {	
		
		GL4 gl = (GL4) GLContext.getCurrentGL();

		String vshaderSource[] = util.readShaderSource("vert.shader.2d");
		String fshaderSource[] = util.readShaderSource("frag.shader.2d");
		
		int vShader = gl.glCreateShader(GL_VERTEX_SHADER);
		int fShader = gl.glCreateShader(GL_FRAGMENT_SHADER);

		gl.glShaderSource(vShader, vshaderSource.length, vshaderSource, null, 0);
		gl.glShaderSource(fShader, fshaderSource.length, fshaderSource, null, 0);

		gl.glCompileShader(vShader);
		gl.glCompileShader(fShader);

		int vfprogram = gl.glCreateProgram();
		gl.glAttachShader(vfprogram, vShader);
		gl.glAttachShader(vfprogram, fShader);
		gl.glLinkProgram(vfprogram);
		return vfprogram;
	}
 
	//Processing line
	private void processLine(float[] V1, float[] V2, int N) {
		
		if (N>0) {
			//Coordinates for midle points
			float[] m1 = new float[2];

			float[] Va = new float[2];
			float[] Vc = new float[2];

			float[] Vb = new float[2];
			
			//calculate midpoint
			m1[0] = (V2[0]+V1[0])/2;
			m1[1] = (V2[1]+V1[1])/2;
			

			// calculate firstThird and second third
			for(int i=0;i<2;i++) {
				Va[i]=V1[i] + (V2[i]-V1[i])/3;
				Vc[i]=V2[i] - (V2[i]-V1[i])/3;
			}

			float[] outVector = new float[2];
			
			float sidelength = (float)Math.sqrt( Math.pow(V2[0]-V1[0] ,2) + Math.pow( V2[1]-V1[1] ,2));		
			float juttingDistance = (float)Math.sqrt( Math.pow(sidelength, 2) - Math.pow(sidelength,2)/4);
		
			float xVec = V2[1]-V1[1];
			float yVec = V2[0]-V1[0];	
			float divider = (float)Math.sqrt(Math.pow(yVec, 2) + Math.pow(xVec, 2));
			outVector[0] = xVec/divider;
			outVector[1] = yVec/divider;
			// calculate Vb
			if (V1[0] < V2[0] && V1[1] < V2[1]) { 
			Vb[1] = m1[1] - outVector[1]*juttingDistance*(1/3f);
			Vb[0] = m1[0] + outVector[0]*juttingDistance*(1/3f);
			} else {
			Vb[1] = m1[1] - outVector[1]*juttingDistance*(1/3f);
			Vb[0] = m1[0] + outVector[0]*juttingDistance*(1/3f);
			} 
			//Recurse
			processLine(V1, Va, N-1);
			processLine(Va, Vb, N-1);
			processLine(Vb, Vc, N-1);
			processLine(Vc, V2, N-1);
		}
		else drawLine(V1, V2); //Draw
		
	}

	private void drawLine(float [] v1, float[] v2) {
		GL4 gl = (GL4) GLContext.getCurrentGL();
		//Store points in backing store
		vertex_positions[0]=v1[0];
		vertex_positions[1]=v1[1];
		vertex_positions[2]=v2[0];
		vertex_positions[3]=v2[1];

		FloatBuffer vertBuf = Buffers.newDirectFloatBuffer(vertex_positions);
		gl.glBufferData(GL_ARRAY_BUFFER, vertBuf.limit()*4, vertBuf, GL_STATIC_DRAW);

		//Draw now
		gl.glDrawArrays(GL_LINES, 0, 2);

	}

	static double numberOfSides = 3*Math.pow(4,N);
	static double perimeter = 3*Math.pow(4,N)*Math.pow(3,-N);
	static double pow = Math.pow((double)4/9,N);
	static float area = (8-3*(float)pow)*(float)1/5;
	
}
