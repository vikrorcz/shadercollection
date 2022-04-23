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

	private SpriteBatch batch;

	private ShaderProgram cloakShader;
	private ShaderProgram maskShader;
	private ShaderProgram rayShader;
	private ShaderProgram planetShader;

	private OrthographicCamera camera;
	private Viewport viewport;

	private FrameBuffer frameBuffer;
	private TextureRegion bufferRegion;

	private float dt;
	private float time;
	private float angle;

	private Texture backTexture;
	private Texture transparentTexture;
	private Texture maskTexture;

	private Sprite backSprite;
	private Sprite cloakSprite;
	private Sprite maskSprite;
	private Sprite spaceSprite;

	private Vector3 position;

	private int locationX;
	private int locationY;

	private TextureRegion textureRegion;

	@Override
	public void create(){

		batch = new SpriteBatch();

		/**
		 * https://github.com/mattdesl/lwjgl-basics/wiki/ShaderLesson4
		 * https://stackoverflow.com/questions/22447270/passing-several-textures-to-shader-in-libgdx
		 *
		 */
		cloakShader = new ShaderProgram(batch.getShader().getVertexShaderSource(), Gdx.files.internal("cloak.frag").readString());
		if (!cloakShader.isCompiled()) {
			Gdx.app.error("cloakShader", "compilation failed:\n" + cloakShader.getLog());
		}

		maskShader = new ShaderProgram(Gdx.files.internal("mask.vert").readString(), Gdx.files.internal("mask.frag").readString());
		if (!maskShader.isCompiled()) {
			Gdx.app.error("maskShader", "compilation failed:\n" + cloakShader.getLog());
		}

		rayShader = new ShaderProgram(Gdx.files.internal("ray.vert").readString(), Gdx.files.internal("ray.frag").readString());
		if (!rayShader.isCompiled()) {
			Gdx.app.error("rayShader", "compilation failed:\n" + rayShader.getLog());
		}

		planetShader = new ShaderProgram(Gdx.files.internal("planet.vert").readString(), Gdx.files.internal("planet.frag").readString());
		if (!planetShader.isCompiled()) {
			Gdx.app.error("planetShader", "compilation failed:\n" + planetShader.getLog());
		}

		ShaderProgram.pedantic = false;


		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f);
		viewport = new ExtendViewport(1920, 1080, camera);
		viewport.apply();

		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 1920, 1080, false);
		bufferRegion = new TextureRegion(frameBuffer.getColorBufferTexture());


		//textures
		//backTexture = new Texture("earth.jpg");

		//for planet
		//backTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
		//maskTexture = new Texture("all.png");
		maskTexture = new Texture("planetmask3.png");
		textureRegion = new TextureRegion(maskTexture,0,0,1920,1080);
		transparentTexture = new Texture("transparent.png");

		//sprites
		cloakSprite = new Sprite();
		//maskSprite = new Sprite(transparentTexture);
		maskSprite = new Sprite(new TextureRegion(maskTexture,0,0,1920,1080));

		backTexture = new Texture("earth4.jpg");
		backSprite = new Sprite(backTexture);
		backSprite.setSize(1920,1080);
		backSprite.getTexture().setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
		backSprite.flip(false,true);


		spaceSprite = new Sprite(new Texture("space2.jpg"));
		spaceSprite.setSize(1920,1080);
		//backSprite.setTexture(backTexture);



	}

	@Override
	public void render(){
		//Gdx.gl.glClearColor(0,0,0,255);
		//batch.setProjectionMatrix(camera.combined);
		camera.update();

		batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE);

		frameBuffer.begin();
		Gdx.gl.glClearColor(1, 0, 0, 0);
		Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
		Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
		Gdx.gl.glEnable(GL20.GL_BLEND_COLOR);
		batch.setProjectionMatrix(camera.combined);

		batch.begin();
		//batch.setColor(0,0,0,255);
		spaceSprite.draw(batch);
		//batch.draw(backTexture,0,0,1920,1080);
		batch.end();
		frameBuffer.end();


		dt += Gdx.graphics.getDeltaTime();
		time += dt;
		float angle = time * (2 * MathUtils.PI);
		if (angle > (2 * MathUtils.PI)){
			angle -= (2 * MathUtils.PI);
		}


		//=================================LIGHT RAYS===============================================
		/*
		batch.begin();
		batch.setShader(rayShader);
		rayShader.bind();
		rayShader.setUniformi("u_texture1", 1);
		rayShader.setUniformf("u_time", dt);
		rayShader.setUniformf("u_amount", 5f);//20;
		rayShader.setUniformf("u_speed", 2f);
		rayShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		batch.draw(backTexture,0,0,1920,1080);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		batch.setShader(null);
		batch.end();
*/
		//=====================================PLANET===============================================

		//backSprite.flip(false, true);
		//backSprite.flip(false, true);
		//backSprite.setSize(1920,1080);

		//backSprite.setSize(800,600);
		//batch.begin();
		//spaceSprite.draw(batch);
		//batch.end();

		batch.begin();
		batch.setShader(planetShader);
		planetShader.bind();
		planetShader.setUniformf("u_time", dt);
		planetShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		backSprite.draw(batch);
		//Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		batch.setShader(null);
		batch.end();



		//PLANET MASK
		maskSprite.setSize(1920,1080);
		maskSprite.setPosition(0,0);
		batch.begin();
		batch.setShader(maskShader);
		maskShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		maskShader.setUniformi("u_texture1", 1);
		maskShader.setUniformi("u_mask", 2);
		bufferRegion.getTexture().bind(1);
		textureRegion.getTexture().bind(2);
		maskSprite.draw(batch);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		batch.setShader(null);
		batch.end();



		position = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(),0));

		locationX = (int) position.x;
		locationY = (int) position.y;


		//bufferRegion.setRegion(0,0,800,600);
/*
		//distorsion shader
		bufferRegion.setRegion(locationX - 100,locationY - 100,200,200);
		cloakSprite.setRegion(bufferRegion);
		cloakSprite.flip(false, true);
		cloakSprite.setSize(200,200);
		cloakSprite.setPosition(locationX - 100,locationY - 100);
		batch.begin();
		batch.setShader(cloakShader);
		cloakShader.bind();
		cloakShader.setUniformf("u_time", -dt);
		cloakShader.setUniformf("u_amount", 10f);//20;
		cloakShader.setUniformf("u_speed", .5f);
		cloakSprite.draw(batch);
		batch.setShader(null);


		//mask shader
		maskSprite.setSize(200,200);
		maskSprite.setPosition(locationX - 100,locationY - 100);
		batch.setShader(maskShader);
		maskShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
		maskShader.setUniformi("u_texture1", 1);
		maskShader.setUniformi("u_mask", 2);
		bufferRegion.getTexture().bind(1);
		textureRegion.getTexture().bind(2);
		maskSprite.draw(batch);
		Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
		batch.setShader(null);
		batch.end();
*/


	}

	@Override
	public void resize(int width, int height){
		viewport.update(width, height);
		camera.setToOrtho(false, 1920, 1080);
	}

	@Override
	public void dispose(){
		batch.dispose();
		backTexture.dispose();
		maskTexture.dispose();
		transparentTexture.dispose();
		maskSprite.getTexture().dispose();
		//cloakSprite.getTexture().dispose();
		frameBuffer.dispose();
		bufferRegion.getTexture().dispose();
		planetShader.dispose();
		rayShader.dispose();
		maskShader.dispose();
		cloakShader.dispose();
		spaceSprite.getTexture().dispose();
	}
}




