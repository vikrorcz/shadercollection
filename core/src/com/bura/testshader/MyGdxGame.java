package com.bura.testshader;

import static com.badlogic.gdx.graphics.GL20.GL_TEXTURE1;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class MyGdxGame extends ApplicationAdapter {


	//CHANGE DESIRED SHADER HERE
	private final int DESIRED_SHADER = 1;
	/*
	 *  |=====================================================================|
	 *  |    DESIRED_SHADER    |             		type             	      |
	 *  |----------------------|----------------------------------------------|
	 *  |          1           |   bubble wave shader                         |
	 *  |          2           |   texture region shape changing wave shader  |
	 *  |          3           |   planet shader							  |
	 *  |          4           |   water waves + rays shader                  |
	 *  |=====================================================================|
	 */


	//DO NOT CHANGE THIS
	private SpriteBatch batch;

	private ShaderProgram cloakShader;
	private ShaderProgram maskShader;
	private ShaderProgram rayShader;
	private ShaderProgram planetShader;
	private ShaderProgram waterShader;

	private OrthographicCamera camera;
	private Viewport viewport;

	private FrameBuffer frameBuffer;
	private TextureRegion bufferRegion;

	private float dt;
	private float time;
	private float timeToChangeTexture;
	private int currentTexture = 1;

	private Texture backTexture;
	private Texture transparentTexture;
	private Texture maskTexture;
	private Texture rgbaNoiseTexture;

	private Sprite backSprite;
	private Sprite cloakSprite;
	private Sprite maskSprite;
	private Sprite spaceSprite;


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

		waterShader = new ShaderProgram(Gdx.files.internal("water.vert").readString(), Gdx.files.internal("water.frag").readString());
		if (!waterShader.isCompiled()) {
			Gdx.app.error("waterShader", "compilation failed:\n" + waterShader.getLog());
		}


		ShaderProgram.pedantic = false;


		camera = new OrthographicCamera();
		camera.setToOrtho(false, Gdx.graphics.getWidth() / 2.0f, Gdx.graphics.getHeight() / 2.0f);
		viewport = new ExtendViewport(1920, 1080, camera);
		viewport.apply();

		frameBuffer = new FrameBuffer(Pixmap.Format.RGBA8888, 1920, 1080, false);
		bufferRegion = new TextureRegion(frameBuffer.getColorBufferTexture());



		switch (DESIRED_SHADER) {
			case 1:
				backTexture = new Texture("water.jpg");
				maskTexture = new Texture("mask.png");
				break;

			case 2:
				backTexture = new Texture("water.jpg");
				maskTexture = new Texture("all.png");

				break;

			case 3:
				backTexture = new Texture("earth4.jpg");
				maskTexture = new Texture("planetmask3.png");
				break;

			case 4:
				rgbaNoiseTexture = new Texture("noise2.png");
				rgbaNoiseTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
				rgbaNoiseTexture.setFilter(Texture.TextureFilter.MipMap, Texture.TextureFilter.MipMap);
				maskTexture = new Texture("planetmask3.png");
				backTexture = new Texture("kepler1.png");
				break;

		}

		TextureRegion textureRegion = new TextureRegion(maskTexture, 0, 0, 1920, 1080);
		transparentTexture = new Texture("transparent.png");

		cloakSprite = new Sprite();
		maskSprite = new Sprite(transparentTexture);

		if (DESIRED_SHADER == 2) {
			maskSprite = new Sprite(new TextureRegion(maskTexture,0,0,100,100));
		}


		backSprite = new Sprite(backTexture);
		backSprite.setSize(1920,1080);
		backSprite.getTexture().setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.ClampToEdge);
		backSprite.flip(false,true);


		spaceSprite = new Sprite(new Texture("space2.jpg"));
		spaceSprite.setSize(1920,1080);

	}


	@Override
	public void render(){
		batch.setProjectionMatrix(camera.combined);
		camera.update();

		batch.setBlendFunctionSeparate(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA, GL20.GL_ONE, GL20.GL_ONE);

		frameBuffer.begin();
		batch.begin();
		batch.draw(backTexture,0,0,1920,1080);
		batch.end();
		frameBuffer.end();


		batch.begin();
		batch.draw(backTexture,0,0,1920,1080);
		batch.end();

		Vector3 position = camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));

		int locationX = (int) position.x;
		int locationY = (int) position.y;


		dt += Gdx.graphics.getDeltaTime();
		time += dt;
		float angle = time * (2 * MathUtils.PI);
		if (angle > (2 * MathUtils.PI)){
			angle -= (2 * MathUtils.PI);
		}

		switch (DESIRED_SHADER) {
			case 1:
				//distorsion shader
				bufferRegion.setRegion(locationX - 100, locationY - 100,200,200);
				cloakSprite.setRegion(bufferRegion);
				cloakSprite.flip(false, true);
				cloakSprite.setSize(200,200);
				cloakSprite.setPosition(locationX - 100, locationY - 100);
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
				maskSprite.setPosition(locationX - 100, locationY - 100);
				batch.setShader(maskShader);
				maskShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
				maskShader.setUniformi("u_texture1", 1);
				maskShader.setUniformi("u_mask", 2);
				bufferRegion.getTexture().bind(1);
				maskTexture.bind(2);
				maskSprite.draw(batch);
				Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
				batch.setShader(null);
				batch.end();
				break;

			case 2:
				timeToChangeTexture += Gdx.graphics.getDeltaTime();
				if (timeToChangeTexture > 1) {
					timeToChangeTexture--;

					switch (currentTexture) {
						case 1:
							maskSprite = new Sprite(new TextureRegion(maskTexture,100,0,100,100));
							currentTexture = 2;
							break;

						case 2:
							maskSprite = new Sprite(new TextureRegion(maskTexture,100,100,100,100));
							currentTexture = 3;
							break;

						case 3:
							maskSprite = new Sprite(new TextureRegion(maskTexture,0,100,100,100));
							currentTexture = 4;
							break;

						case 4:
							maskSprite = new Sprite(new TextureRegion(maskTexture,0,0,100,100));
							currentTexture = 1;
							break;
					}
				}

				//distorsion shader
				bufferRegion.setRegion(locationX - 100, locationY - 100,200,200);
				cloakSprite.setRegion(bufferRegion);
				cloakSprite.flip(false, true);
				cloakSprite.setSize(200,200);
				cloakSprite.setPosition(locationX - 100, locationY - 100);
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
				maskSprite.setPosition(locationX - 100, locationY - 100);
				batch.setShader(maskShader);
				maskShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
				maskShader.setUniformi("u_texture1", 1);
				maskShader.setUniformi("u_mask", 2);
				bufferRegion.getTexture().bind(1);
				maskTexture.bind(2);
				maskSprite.draw(batch);
				Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
				batch.setShader(null);
				batch.end();
				break;

			case 3:
				batch.begin();
				spaceSprite.draw(batch);
				batch.end();

				batch.begin();
				batch.setShader(planetShader);
				planetShader.bind();
				planetShader.setUniformf("u_time", dt);
				planetShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
				backSprite.draw(batch);
				Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
				batch.setShader(null);
				batch.end();
				break;

			case 4:
				batch.begin();
				batch.setShader(waterShader);
				waterShader.bind();
				waterShader.setUniformf("u_time", dt);
				waterShader.setUniformf("u_amount", 5f);//20;
				waterShader.setUniformf("u_speed", 2f);
				waterShader.setUniformf("resolution", new Vector2(Gdx.graphics.getWidth(), Gdx.graphics.getHeight()));
				waterShader.setUniformi("u_texture2", 2);//rgbanoise
				rgbaNoiseTexture.bind(2);
				Gdx.gl.glActiveTexture(GL_TEXTURE1);
				backSprite.draw(batch);
				Gdx.gl.glActiveTexture(GL20.GL_TEXTURE0);
				batch.setShader(null);
				batch.end();
				break;
		}


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
		cloakSprite.getTexture().dispose();
		frameBuffer.dispose();
		bufferRegion.getTexture().dispose();
	}
}




