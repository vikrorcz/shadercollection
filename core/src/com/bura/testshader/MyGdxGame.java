package com.bura.testshader;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.StretchViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

import org.graalvm.compiler.core.common.type.ArithmeticOpTable;

import java.util.ArrayList;

public class MyGdxGame extends ApplicationAdapter {
	final String VERT =
			"attribute vec4 "+ShaderProgram.POSITION_ATTRIBUTE+";\n" +
					"attribute vec4 "+ShaderProgram.COLOR_ATTRIBUTE+";\n" +
					"attribute vec2 "+ShaderProgram.TEXCOORD_ATTRIBUTE+"0;\n" +

					"uniform mat4 u_projTrans;\n" +
					" \n" +
					"varying vec4 vColor;\n" +
					"varying vec2 vTexCoord;\n" +

					"void main() {\n" +
					"	vColor = "+ShaderProgram.COLOR_ATTRIBUTE+";\n" +
					"	vTexCoord = "+ShaderProgram.TEXCOORD_ATTRIBUTE+"0;\n" +
					"	gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
					"}";

	final String FRAG =
			//GL ES specific stuff
			"#ifdef GL_ES\n" //
					+ "#define LOWP lowp\n" //
					+ "precision mediump float;\n" //
					+ "#else\n" //
					+ "#define LOWP \n" //
					+ "#endif\n" + //
					"varying LOWP vec4 vColor;\n" +
					"varying vec2 vTexCoord;\n" +
					"uniform sampler2D u_texture;\n" +
					"uniform sampler2D u_texture1;\n" +
					"uniform sampler2D u_mask;\n" +
					"void main(void) {\n" +
					"	//sample the colour from the first texture\n" +
					"	vec4 texColor0 = texture2D(u_texture, vTexCoord);\n" +
					"\n" +
					"	//sample the colour from the second texture\n" +
					"	vec4 texColor1 = texture2D(u_texture1, vTexCoord);\n" +
					"\n" +
					"	//get the mask; we will only use the alpha channel\n" +
					"	float mask = texture2D(u_mask, vTexCoord).a;\n" +
					"\n" +
					"	//interpolate the colours based on the mask\n" +
					"	gl_FragColor = vColor * mix(texColor0, texColor1, mask);\n" +
					"}";


	SpriteBatch batch;

	FrameBuffer frameBuffer;
	TextureRegion bufferTexture;
	Texture back;
	Texture mask;
	Texture dirt;

	ShaderProgram cloakShader;
	ShaderProgram maskShader;

	OrthographicCamera camera;
	Viewport viewport;

	float dt;
	float time;

	float scaleX;
	float scaleY;

	Vector3 coords;
	ArrayList<TextureRegion> heatRegions;
	ArrayList<Vector2> heatCoords;
	ArrayList<Vector2> heatDimensions;
	Matrix4 idt;

	Texture texture;

	@Override
	public void create(){

		batch = new SpriteBatch();
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 800, 600, false);
		bufferTexture = new TextureRegion(frameBuffer.getColorBufferTexture());
		bufferTexture.flip(false,true);

		back = new Texture("a-water.jpg");
		mask =  new Texture("mask4.png");
		dirt =  new Texture("ss.png");
		cloakShader = new ShaderProgram(batch.getShader().getVertexShaderSource(), Gdx.files.internal("cloak.frag").readString());
		//cloakShader = new ShaderProgram(Gdx.files.internal("cloak.vert"), Gdx.files.internal("cloak.frag"));
		if (!cloakShader.isCompiled()) {
			Gdx.app.error("cloakShader", "compilation failed:\n" + cloakShader.getLog());
		}


		maskShader = new ShaderProgram(VERT, FRAG);
		ShaderProgram.pedantic = false;

		camera = new OrthographicCamera(800,600);
		camera.setToOrtho(false);
		//viewport = new StretchViewport(800,  600 , camera);
		//viewport.apply();

		//camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
		camera.update();

		coords = new Vector3(0,0,0);
		heatRegions = new ArrayList<TextureRegion>();
		heatCoords = new ArrayList<Vector2>();
		heatDimensions = new ArrayList<Vector2>();
		idt = new Matrix4();
		heatRegions.add(new TextureRegion(frameBuffer.getColorBufferTexture()));
		heatCoords.add(new Vector2(0,0));
		heatDimensions.add(new Vector2(32,32));

		scaleX =  Gdx.graphics.getWidth() / camera.viewportWidth ; //floats that convert the width and height in world
		scaleY =   Gdx.graphics.getHeight() / camera.viewportHeight;//to width and height on screen

		maskShader.setUniformi("u_texture1", 1);
		maskShader.setUniformi("u_mask", 2);
		maskShader.end();

		//bind mask to glActiveTexture(GL_TEXTURE2)
		mask.bind(2);

		//bind dirt to glActiveTexture(GL_TEXTURE1)
		dirt.bind(1);

		//now we need to reset glActiveTexture to zero!!!! since sprite batch does not do this for us
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
	}

	private void renderFBO(){

		frameBuffer.begin();
		batch.begin();
		Gdx.gl.glClearColor(0f,0f,0f,0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.enableBlending();
		//batch.setColor(Color.CYAN);

		//camera.setToOrtho(true);
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.draw(back,0,0,800,600);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.end();
		frameBuffer.end();
		///camera.setToOrtho(false);
	}

	@Override
	public void render(){

		//renderFBO();

		//camera.update();

		batch.begin();
		//batch.setColor(Color.WHITE);
		batch.draw(back,0,0,800,600);
		batch.end();



		dt += Gdx.graphics.getDeltaTime();
		//time += dt;
		//float angle = time * (2 * MathUtils.PI);
		//if (angle > (2 * MathUtils.PI)){
		//	angle -= (2 * MathUtils.PI);
		//}

		batch.setShader(cloakShader);
		cloakShader.bind();
		//cloakShader.setUniformf("timedelta", -angle);
		cloakShader.setUniformf("u_amount", 20);
		cloakShader.setUniformf("u_speed", 3f);
		cloakShader.setUniformf("u_time", dt);

		batch.begin();
		Vector3 tempCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(tempCoords);
		Pixmap pixmap = Pixmap.createFromFrameBuffer((int) tempCoords.x - 100, (int) tempCoords.y - 100,200,200);
		texture = new Texture(pixmap);
		Sprite sprite = new Sprite(texture);
		sprite.flip(false, true);
		sprite.setPosition((int) tempCoords.x - 100, (int) tempCoords.y - 100);
		sprite.draw(batch);
		//batch.draw(texture,0,0);

		//batch.draw(bufferTexture.getTexture(),0,0,800,600);
		//batch.draw(bufferTexture.getTexture(),Gdx.input.getX() - 100,Gdx.input.getY() - 100,200,200);
		batch.setShader(null);
		batch.end();




		batch.begin();
		batch.setShader(maskShader);
		maskShader.bind();
		maskShader.setUniformi("u_texture1", 1);
		maskShader.setUniformi("u_mask", 2);

		texture.bind(1);
		mask.bind(2);
		Sprite sprite2 = new Sprite(dirt);
		sprite2.setSize(200,200);
		sprite2.flip(false, true);
		sprite2.setPosition((int) tempCoords.x - 100, (int) tempCoords.y - 100);
		sprite2.draw(batch);
		//batch.draw(dirt,(int) tempCoords.x - 100, (int) tempCoords.y - 100,200,200);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		batch.setShader(null);
		batch.end();




	}


	@Override
	public void resize(int width, int height){
		camera.update();
	}








/*
	SpriteBatch batch;

	FrameBuffer frameBuffer;
	TextureRegion bufferTexture;
	Texture back;

	ShaderProgram cloakShader;

	OrthographicCamera camera;
	Viewport viewport;

	float dt;
	float time;

	float scaleX;
	float scaleY;

	Vector3 coords;
	ArrayList<TextureRegion> heatRegions;
	ArrayList<Vector2> heatCoords;
	ArrayList<Vector2> heatDimensions;
	Matrix4 idt;

	Texture texture;

	@Override
	public void create(){

		batch = new SpriteBatch();
		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 800, 600, false);
		bufferTexture = new TextureRegion(frameBuffer.getColorBufferTexture());
		bufferTexture.flip(false,true);

		back = new Texture("a-water.jpg");
		cloakShader = new ShaderProgram(batch.getShader().getVertexShaderSource(), Gdx.files.internal("cloak.frag").readString());
		//cloakShader = new ShaderProgram(Gdx.files.internal("cloak.vert"), Gdx.files.internal("cloak.frag"));
		if (!cloakShader.isCompiled()) {
			Gdx.app.error("cloakShader", "compilation failed:\n" + cloakShader.getLog());
		}

		ShaderProgram.pedantic = false;

		camera = new OrthographicCamera(800,600);
		camera.setToOrtho(false);
		//viewport = new StretchViewport(800,  600 , camera);
		//viewport.apply();
//
		//camera.position.set(camera.viewportWidth / 2, camera.viewportHeight / 2, 0);
		camera.update();

		coords = new Vector3(0,0,0);
		heatRegions = new ArrayList<TextureRegion>();
		heatCoords = new ArrayList<Vector2>();
		heatDimensions = new ArrayList<Vector2>();
		idt = new Matrix4();
		heatRegions.add(new TextureRegion(frameBuffer.getColorBufferTexture()));
		heatCoords.add(new Vector2(0,0));
		heatDimensions.add(new Vector2(32,32));

		scaleX =  Gdx.graphics.getWidth() / camera.viewportWidth ; //floats that convert the width and height in world
		scaleY =   Gdx.graphics.getHeight() / camera.viewportHeight;//to width and height on screen


	}

	private void renderFBO(){

		frameBuffer.begin();
		batch.begin();
		Gdx.gl.glClearColor(0f,0f,0f,0f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		batch.enableBlending();
		//batch.setColor(Color.CYAN);

		//camera.setToOrtho(true);
		camera.update();
		batch.setProjectionMatrix(camera.combined);
		batch.draw(back,0,0,800,600);
		batch.setBlendFunction(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		batch.end();
		frameBuffer.end();
		///camera.setToOrtho(false);
	}

	@Override
	public void render(){

		renderFBO();

		//camera.update();

		batch.begin();
		//batch.setColor(Color.WHITE);
		batch.draw(back,0,0,800,600);
		batch.end();



		dt += Gdx.graphics.getDeltaTime();
		//time += dt;
		//float angle = time * (2 * MathUtils.PI);
		//if (angle > (2 * MathUtils.PI)){
		//	angle -= (2 * MathUtils.PI);
		//}

		batch.setShader(cloakShader);
		cloakShader.bind();
		//cloakShader.setUniformf("timedelta", -angle);
		cloakShader.setUniformf("u_amount", 20);
		cloakShader.setUniformf("u_speed", 3f);
		cloakShader.setUniformf("u_time", dt);

		batch.begin();
		Vector3 tempCoords = new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0);
		camera.unproject(tempCoords);
		Pixmap pixmap = Pixmap.createFromFrameBuffer((int) tempCoords.x - 100, (int) tempCoords.y - 100,200,200);
		texture = new Texture(pixmap);
		Sprite sprite = new Sprite(texture);
		sprite.flip(false, true);
		//sprite.setColor(Color.CYAN);
		sprite.setPosition((int) tempCoords.x - 100, (int) tempCoords.y - 100);
		sprite.draw(batch);
		//batch.draw(texture,0,0);

		//batch.draw(bufferTexture.getTexture(),0,0,800,600);
		//batch.draw(bufferTexture.getTexture(),Gdx.input.getX() - 100,Gdx.input.getY() - 100,200,200);
		batch.setShader(null);
		batch.end();
	}


	@Override
	public void resize(int width, int height){
		camera.update();
	}
*/


/*
	SpriteBatch batch;
	Texture img;
	Texture back;
	Texture refract;


	private ShaderProgram cloakShader;

	float time;

	@Override
	public void create () {
		batch = new SpriteBatch();
		img = new Texture("normal.png");
		back = new Texture("a-water.jpg");
		refract = new Texture("refraction.png");

		cloakShader = new ShaderProgram(Gdx.files.internal("water.vert"), Gdx.files.internal("water.frag"));
		if (!cloakShader.isCompiled()) {
			Gdx.app.error("fontShader", "compilation failed:\n" + cloakShader.getLog());
		}

		ShaderProgram.pedantic = false;
	}

	@Override
	public void render () {
		ScreenUtils.clear(1, 0, 0, 1);
		batch.begin();

		batch.draw(back, 0, 0,500,500);

		//time += Gdx.graphics.getDeltaTime();
		batch.setShader(cloakShader);
		cloakShader.bind();
		cloakShader.setUniformf("scrollOffset",1f);
		cloakShader.setUniformf("refractionAmount",1f);
		cloakShader.setUniformi("u_texture",1);
		cloakShader.setUniformi("u_diffuse",2);
		cloakShader.setUniformi("u_reflection",3);
		img.bind(1);
		back.bind(2);
		refract.bind(3);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);


		//cloakShader.setUniformf("u_amount", 10);
		//cloakShader.setUniformf("u_speed", .5f);
		//cloakShader.setUniformf("u_time", time);
		batch.draw(img, 0, 0,500,500);
		batch.setShader(null);



		batch.end();




		//tex0 will be bound when we call SpriteBatch.draw

		//batch = new SpriteBatch(1000, shader);
		//batch.setShader(shader);
	}
	
	@Override
	public void dispose () {
		batch.dispose();
		img.dispose();
	}
*/

		/////////////////////////////////////////1
/*
	final String VERT =
			"attribute vec4 "+ShaderProgram.POSITION_ATTRIBUTE+";\n" +
					"attribute vec4 "+ShaderProgram.COLOR_ATTRIBUTE+";\n" +
					"attribute vec2 "+ShaderProgram.TEXCOORD_ATTRIBUTE+"0;\n" +

					"uniform mat4 u_projTrans;\n" +
					" \n" +
					"varying vec4 vColor;\n" +
					"varying vec2 vTexCoord;\n" +

					"void main() {\n" +
					"	vColor = "+ShaderProgram.COLOR_ATTRIBUTE+";\n" +
					"	vTexCoord = "+ShaderProgram.TEXCOORD_ATTRIBUTE+"0;\n" +
					"	gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
					"}";

	final String FRAG =
			"#ifdef GL_ES\n" +
					"#define LOWP lowp\n" +
					"precision mediump float;\n" +
					"#else\n" +
					"#define LOWP \n" +
					"#endif\n" +
					"varying LOWP vec4 vColor;\n" +
					"varying vec2 vTexCoord;\n" +
					"uniform sampler2D u_texture;\n" +
					"uniform sampler2D u_texture1;\n" +
					"uniform sampler2D u_mask;\n" +
					"\n" +
					"\n" +
					"\n" +
					"uniform float time;\n" +
					"\n" +
					"	/////////////////////////////////////////////////////////////////////////\n" +
					"	/////////////////// SIMPLEX NOISE FROM WEBGL-NOISE //////////////////////\n" +
					"	/////////////////////////////////////////////////////////////////////////\n" +
					"	//            https://github.com/ashima/webgl-noise/wiki               //\n" +
					"	/////////////////////////////////////////////////////////////////////////\n" +
					"\n" +
					"vec3 mod289(vec3 x) {\n" +
					"  return x - floor(x * (1.0 / 289.0)) * 289.0;\n" +
					"}\n" +
					"\n" +
					"vec2 mod289(vec2 x) {\n" +
					"  return x - floor(x * (1.0 / 289.0)) * 289.0;\n" +
					"}\n" +
					"\n" +
					"vec3 permute(vec3 x) {\n" +
					"  return mod289(((x*34.0)+1.0)*x);\n" +
					"}\n" +
					"\n" +
					"float snoise(vec2 v) {\n" +
					"  const vec4 C = vec4(0.211324865405187,  // (3.0-sqrt(3.0))/6.0\n" +
					"                      0.366025403784439,  // 0.5*(sqrt(3.0)-1.0)\n" +
					"                     -0.577350269189626,  // -1.0 + 2.0 * C.x\n" +
					"                      0.024390243902439); // 1.0 / 41.0\n" +
					"// First corner\n" +
					"  vec2 i  = floor(v + dot(v, C.yy) );\n" +
					"  vec2 x0 = v -   i + dot(i, C.xx);\n" +
					"\n" +
					"// Other corners\n" +
					"  vec2 i1;\n" +
					"  //i1.x = step( x0.y, x0.x ); // x0.x > x0.y ? 1.0 : 0.0\n" +
					"  //i1.y = 1.0 - i1.x;\n" +
					"  i1 = (x0.x > x0.y) ? vec2(1.0, 0.0) : vec2(0.0, 1.0);\n" +
					"  // x0 = x0 - 0.0 + 0.0 * C.xx ;\n" +
					"  // x1 = x0 - i1 + 1.0 * C.xx ;\n" +
					"  // x2 = x0 - 1.0 + 2.0 * C.xx ;\n" +
					"  vec4 x12 = x0.xyxy + C.xxzz;\n" +
					"  x12.xy -= i1;\n" +
					"\n" +
					"// Permutations\n" +
					"  i = mod289(i); // Avoid truncation effects in permutation\n" +
					"  vec3 p = permute( permute( i.y + vec3(0.0, i1.y, 1.0 ))\n" +
					"		+ i.x + vec3(0.0, i1.x, 1.0 ));\n" +
					"\n" +
					"  vec3 m = max(0.5 - vec3(dot(x0,x0), dot(x12.xy,x12.xy), dot(x12.zw,x12.zw)), 0.0);\n" +
					"  m = m*m ;\n" +
					"  m = m*m ;\n" +
					"\n" +
					"// Gradients: 41 points uniformly over a line, mapped onto a diamond.\n" +
					"// The ring size 17*17 = 289 is close to a multiple of 41 (41*7 = 287)\n" +
					"\n" +
					"  vec3 x = 2.0 * fract(p * C.www) - 1.0;\n" +
					"  vec3 h = abs(x) - 0.5;\n" +
					"  vec3 ox = floor(x + 0.5);\n" +
					"  vec3 a0 = x - ox;\n" +
					"\n" +
					"// Normalise gradients implicitly by scaling m\n" +
					"// Approximation of: m *= inversesqrt( a0*a0 + h*h );\n" +
					"  m *= 1.79284291400159 - 0.85373472095314 * ( a0*a0 + h*h );\n" +
					"\n" +
					"// Compute final noise value at P\n" +
					"  vec3 g;\n" +
					"  g.x  = a0.x  * x0.x  + h.x  * x0.y;\n" +
					"  g.yz = a0.yz * x12.xz + h.yz * x12.yw;\n" +
					"  return 130.0 * dot(m, g);\n" +
					"}\n" +
					"\n" +
					"\n" +
					"	/////////////////////////////////////////////////////////////////////////\n" +
					"	////////////////////       END SIMPLEX NOISE     ////////////////////////\n" +
					"	/////////////////////////////////////////////////////////////////////////\n" +
					"	\n" +
					"	\n" +
					"void main(void) {\n" +
					"	//sample the colour from the first texture\n" +
					"	vec4 texColor0 = texture2D(u_texture, vTexCoord);\n" +
					"	//sample the colour from the second texture\n" +
					"	vec4 texColor1 = texture2D(u_texture1, vTexCoord);\n" +
					"	\n" +
					"	//pertube texcoord by x and y\n" +
					"	vec2 distort = 0.2 * vec2(snoise(vTexCoord + vec2(0.0, time/3.0)),\n" +
					"                              snoise(vTexCoord + vec2(time/3.0, 0.0)) );\n" +
					"	\n" +
					"	//get the mask; we will only use the alpha channel\n" +
					"	float mask = texture2D(u_mask, vTexCoord + distort).a;\n" +
					"\n" +
					"	//interpolate the colours based on the mask\n" +
					"	gl_FragColor = vColor * mix(texColor0, texColor1, mask);\n" +
					"}";


	Texture tex0, tex1, mask;
	SpriteBatch batch;
	OrthographicCamera cam;
	ShaderProgram shader;
	float time;
	BitmapFont fps;

	@Override
	public void create() {
		//tex0 = new Texture(Gdx.files.internal("grass.png"));
		tex0 = new Texture(Gdx.files.internal("grass.png"));
		tex1 = new Texture(Gdx.files.internal("dirt.png"));
		//tex1 = new Texture(Gdx.files.internal("dirt.png"));
		mask = new Texture(Gdx.files.internal("mask.png"));

		//important since we aren't using some uniforms and attributes that SpriteBatch expects
		ShaderProgram.pedantic = false;
		//final String FRAG = Gdx.files.internal("data/lesson4b.frag").readString();

		//print it out for clarity
		System.out.println("Vertex Shader:\n-------------\n\n"+VERT);
		System.out.println("\n");
		System.out.println("Fragment Shader:\n-------------\n\n"+FRAG);

		fps = new BitmapFont();

		shader = new ShaderProgram(VERT, FRAG);
		if (!shader.isCompiled()) {
			System.err.println(shader.getLog());
			System.exit(0);
		}
		if (shader.getLog().length()!=0)
			System.out.println(shader.getLog());


		shader.begin();
		shader.setUniformi("u_texture1", 1);
		shader.setUniformi("u_mask", 2);
		shader.end();

		//bind mask to glActiveTexture(GL_TEXTURE2)
		mask.bind(2);

		//bind dirt to glActiveTexture(GL_TEXTURE1)
		tex1.bind(1);

		//now we need to reset glActiveTexture to zero!!!! since sprite batch does not do this for us
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);

		//tex0 will be bound when we call SpriteBatch.draw

		batch = new SpriteBatch(1000, shader);
		batch.setShader(shader);

		cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.setToOrtho(false);
	}

	@Override
	public void resize(int width, int height) {
		cam.setToOrtho(false, width, height);
		batch.setProjectionMatrix(cam.combined);
	}

	@Override
	public void render() {
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		batch.begin();
		shader.setUniformf("time", time+=Gdx.graphics.getDeltaTime());
		batch.draw(tex0, 0, 0);
		batch.end();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		batch.dispose();
		shader.dispose();
		tex0.dispose();
	}
*/


		/////////////////////////////////////////1

/*
	/////////////////////////////////////////2
	Texture tex, tex2;

	SpriteBatch batch;
	OrthographicCamera cam;

	ShaderProgram blurShader;
	FrameBuffer blurTargetA, blurTargetB;
	TextureRegion fboRegion;

	public static final int FBO_SIZE = 1024;

	public static final float MAX_BLUR = 2f;

	BitmapFont fps;

	final String VERT =
			"attribute vec4 "+ShaderProgram.POSITION_ATTRIBUTE+";\n" +
					"attribute vec4 "+ShaderProgram.COLOR_ATTRIBUTE+";\n" +
					"attribute vec2 "+ShaderProgram.TEXCOORD_ATTRIBUTE+"0;\n" +

					"uniform mat4 u_projTrans;\n" +
					" \n" +
					"varying vec4 vColor;\n" +
					"varying vec2 vTexCoord;\n" +

					"void main() {\n" +
					"	vColor = "+ShaderProgram.COLOR_ATTRIBUTE+";\n" +
					"	vTexCoord = "+ShaderProgram.TEXCOORD_ATTRIBUTE+"0;\n" +
					"	gl_Position =  u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";\n" +
					"}";

	final String FRAG =
			"#ifdef GL_ES\n" +
					"#define LOWP lowp\n" +
					"precision mediump float;\n" +
					"#else\n" +
					"#define LOWP \n" +
					"#endif\n" +
					"varying LOWP vec4 vColor;\n" +
					"varying vec2 vTexCoord;\n" +
					"\n" +
					"uniform sampler2D u_texture;\n" +
					"uniform float resolution;\n" +
					"uniform float radius;\n" +
					"uniform vec2 dir;\n" +
					"\n" +
					"void main() {\n" +
					"	vec4 sum = vec4(0.0);\n" +
					"	vec2 tc = vTexCoord;\n" +
					"	float blur = radius/resolution; \n" +
					"    \n" +
					"    float hstep = dir.x;\n" +
					"    float vstep = dir.y;\n" +
					"    \n" +
					"	sum += texture2D(u_texture, vec2(tc.x - 4.0*blur*hstep, tc.y - 4.0*blur*vstep)) * 0.05;\n" +
					"	sum += texture2D(u_texture, vec2(tc.x - 3.0*blur*hstep, tc.y - 3.0*blur*vstep)) * 0.09;\n" +
					"	sum += texture2D(u_texture, vec2(tc.x - 2.0*blur*hstep, tc.y - 2.0*blur*vstep)) * 0.12;\n" +
					"	sum += texture2D(u_texture, vec2(tc.x - 1.0*blur*hstep, tc.y - 1.0*blur*vstep)) * 0.15;\n" +
					"	\n" +
					"	sum += texture2D(u_texture, vec2(tc.x, tc.y)) * 0.16;\n" +
					"	\n" +
					"	sum += texture2D(u_texture, vec2(tc.x + 1.0*blur*hstep, tc.y + 1.0*blur*vstep)) * 0.15;\n" +
					"	sum += texture2D(u_texture, vec2(tc.x + 2.0*blur*hstep, tc.y + 2.0*blur*vstep)) * 0.12;\n" +
					"	sum += texture2D(u_texture, vec2(tc.x + 3.0*blur*hstep, tc.y + 3.0*blur*vstep)) * 0.09;\n" +
					"	sum += texture2D(u_texture, vec2(tc.x + 4.0*blur*hstep, tc.y + 4.0*blur*vstep)) * 0.05;\n" +
					"\n" +
					"	gl_FragColor = vColor * vec4(sum.rgb, 1.0);\n" +
					"}";

	@Override
	public void create() {
		tex = new Texture(Gdx.files.internal("slider.png"));
		tex2 = new Texture(Gdx.files.internal("ptsans_00.png"));
		tex.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
		tex2.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

		//important since we aren't using some uniforms and attributes that SpriteBatch expects
		ShaderProgram.pedantic = false;

		blurShader = new ShaderProgram(VERT, FRAG);
		if (!blurShader.isCompiled()) {
			System.err.println(blurShader.getLog());
			System.exit(0);
		}
		if (blurShader.getLog().length()!=0)
			System.out.println(blurShader.getLog());

		//setup uniforms for our shader
		blurShader.begin();
		blurShader.setUniformf("dir", 0f, 0f);
		blurShader.setUniformf("resolution", FBO_SIZE);
		blurShader.setUniformf("radius", 1f);
		blurShader.end();

		blurTargetA = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_SIZE, FBO_SIZE, false);
		blurTargetB = new FrameBuffer(Pixmap.Format.RGBA8888, FBO_SIZE, FBO_SIZE, false);
		fboRegion = new TextureRegion(blurTargetA.getColorBufferTexture());
		fboRegion.flip(false, true);

		batch = new SpriteBatch();

		fps = new BitmapFont();

		cam = new OrthographicCamera(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
		cam.setToOrtho(false);
	}

	@Override
	public void resize(int width, int height) {
	}

	void resizeBatch(int width, int height) {
		cam.setToOrtho(false, width, height);
		batch.setProjectionMatrix(cam.combined);
	}

	void renderEntities(SpriteBatch batch) {
		batch.draw(tex, 0, 0);
		batch.draw(tex2, tex.getWidth()+5, 30);
	}

	@Override
	public void render() {
		//Start rendering to an offscreen color buffer
		blurTargetA.begin();

		//Clear the offscreen buffer with an opaque background
		Gdx.gl.glClearColor(0.5f, 0.5f, 0.5f, 1f);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

		//before rendering, ensure we are using the default shader
		batch.setShader(null);

		//resize the batch projection matrix before drawing with it
		resizeBatch(FBO_SIZE, FBO_SIZE);

		//now we can start drawing...
		batch.begin();

		//draw our scene here
		renderEntities(batch);

		//finish rendering to the offscreen buffer
		batch.flush();

		//finish rendering to the offscreen buffer
		blurTargetA.end();

		//now let's start blurring the offscreen image
		batch.setShader(blurShader);

		//since we never called batch.end(), we should still be drawing
		//which means are blurShader should now be in use

		//ensure the direction is along the X-axis only
		blurShader.setUniformf("dir", 1f, 0f);

		//update blur amount based on touch input
		float mouseXAmt = Gdx.input.getX() / (float)Gdx.graphics.getWidth();
		blurShader.setUniformf("radius", mouseXAmt * MAX_BLUR);

		//our first blur pass goes to target B
		blurTargetB.begin();

		//we want to render FBO target A into target B
		fboRegion.setTexture(blurTargetA.getColorBufferTexture());

		//draw the scene to target B with a horizontal blur effect
		batch.draw(fboRegion, 0, 0);

		//flush the batch before ending the FBO
		batch.flush();

		//finish rendering target B
		blurTargetB.end();

		//now we can render to the screen using the vertical blur shader

		//update our projection matrix with the screen size
		resizeBatch(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

		//update the blur only along Y-axis
		blurShader.setUniformf("dir", 0f, 1f);

		//update the Y-axis blur radius
		float mouseYAmt = Gdx.input.getY() / (float)Gdx.graphics.getHeight();
		blurShader.setUniformf("radius", mouseYAmt * MAX_BLUR);

		//draw target B to the screen with a vertical blur effect
		fboRegion.setTexture(blurTargetB.getColorBufferTexture());
		batch.draw(fboRegion, 0, 0);

		//reset to default shader without blurs
		batch.setShader(null);

		//draw FPS
		fps.draw(batch, String.valueOf(Gdx.graphics.getFramesPerSecond()), 5, Gdx.graphics.getHeight()-5);

		//finally, end the batch since we have reached the end of the frame
		batch.end();
	}

	@Override
	public void pause() {

	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {
		batch.dispose();
		blurShader.dispose();
		tex.dispose();
		tex2.dispose();
	}
	/////////////////////////////////////////2
*/
/*
	private Stage stage;

	@Override
	public void create () {
		stage = new Stage(new ScreenViewport());
		Gdx.input.setInputProcessor(stage);
		Texture texture = new Texture(Gdx.files.internal("dirt.png"));

		stage.addActor(ShockWave.getInstance());

		Image image1 = new Image(texture);
		image1.setPosition(0,0);
		image1.setSize(Gdx.graphics.getWidth(),Gdx.graphics.getHeight());
		image1.addListener(new InputListener() {
			@Override
			public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
				ShockWave.getInstance().start(x,y);
				return true;
			}



		});

		ShockWave.getInstance().addActor(image1);

	}

	@Override
	public void render () {
		Gdx.gl.glClearColor(1, 1, 1, 1);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		stage.act();
		stage.draw();

	}*/


}




