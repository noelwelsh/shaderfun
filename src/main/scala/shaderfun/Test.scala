package shaderfun

import java.nio.ByteBuffer
import java.nio.FloatBuffer

import org.lwjgl.glfw.GLFW
import org.lwjgl._
import org.lwjgl.opengl._

object TheQuadExampleColored extends App {

  val vertexShader = """
    #version 150 core

    in vec4 in_Position;
    in vec4 in_Color;

    out vec4 pass_Color;

    void main(void) {
        gl_Position = in_Position;
        pass_Color = in_Color;
    }
    """

  val fragmentShader = """
    #version 150 core

    in vec4 pass_Color;

    out vec4 out_Color;

    void main(void) {
      float red = smoothstep(-1.0, 1.0, cos(radians(gl_FragCoord.x * 3)));
      float green = smoothstep(-1.0, 1.0, tan(radians(gl_FragCoord.x * gl_FragCoord.y)));
      float blue = smoothstep(-1.0, 1.0, cos(radians(gl_FragCoord.y * 3)));

      out_Color = vec4(red, green, blue, 1.0);
    }
    """

  // Setup variables
  var WINDOW_TITLE: String  = "The Quad: colored"
  val WIDTH = 720
  val HEIGHT = 720

  var window = 0L
  var capabilities: GLCapabilities = null

  // Quad variables
  var vaoId = 0
  var vboId = 0
  var vbocId = 0
  var vboiId = 0
  var indicesCount = 0
  // Shader variables
  var vsId = 0
  var fsId = 0
  var pId = 0


  // Initialize OpenGL (Display)
  this.setupOpenGL()

  System.out.println("OpenGL version: " + GL11.glGetString(GL11.GL_VERSION))

  this.setupQuad()
  this.setupShaders()

  println("Entering loop")
  while ( GLFW.glfwWindowShouldClose(window) == GLFW.GLFW_FALSE ) {
    // Do a single loop (logic/render)
    this.loopCycle()
    GLFW.glfwSwapBuffers(window)
    GLFW.glfwPollEvents()
  }

  // Destroy OpenGL (Display)
  this.destroyOpenGL()

  def setupOpenGL() = {
    GLFW.glfwInit()
    // Configure our window
    GLFW.glfwDefaultWindowHints(); // optional, the current window hints are already the default
    GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE); // the window will stay hidden after creation
    GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, GLFW.GLFW_TRUE); // the window will be resizable
    // Setup OpenGL 3.2 or higher, required for fragment shaders
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MAJOR, 3)
    GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_VERSION_MINOR, 2)
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_FORWARD_COMPAT, GLFW.GLFW_TRUE)
    GLFW.glfwWindowHint(GLFW.GLFW_OPENGL_PROFILE, GLFW.GLFW_OPENGL_CORE_PROFILE)

    window = GLFW.glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", 0, 0)

    // Get the resolution of the primary monitor
    val vidmode = GLFW.glfwGetVideoMode(GLFW.glfwGetPrimaryMonitor());
    // Center our window
    GLFW.glfwSetWindowPos(
      window,
      (vidmode.width() - WIDTH) / 2,
      (vidmode.height() - HEIGHT) / 2
    )

    // Make the OpenGL context current
    GLFW.glfwMakeContextCurrent(window)
    // Enable v-sync
    GLFW.glfwSwapInterval(1)

    // Make the window visible
    GLFW.glfwShowWindow(window)

    capabilities = GL.createCapabilities()
    println(s"Supports OpenGL 3.2? ${capabilities.OpenGL32}")

    GL11.glViewport(0, 0, WIDTH, HEIGHT)

    // Setup an XNA like background color
    GL11.glClearColor(0.4f, 0.6f, 0.9f, 0f)

    // Map the internal OpenGL coordinate system to the entire screen
    GL11.glViewport(0, 0, WIDTH, HEIGHT)
  }

  def setupQuad() = {
    println("Setup Quad")
    // Vertices, the order is not important. XYZW instead of XYZ
    var vertices = Array(
      -1f, 1f, 0f, 1f,
      -1f, -1f, 0f, 1f,
      1f, -1f, 0f, 1f,
      1f, 1f, 0f, 1f
    )
    var verticesBuffer: FloatBuffer = BufferUtils.createFloatBuffer(vertices.length)
    verticesBuffer.put(vertices)
    verticesBuffer.flip()

    var colors: Array[Float] = Array(
      1f, 0f, 0f, 1f,
      0f, 1f, 0f, 1f,
      0f, 0f, 1f, 1f,
      1f, 1f, 1f, 1f
    )
    var colorsBuffer: FloatBuffer = BufferUtils.createFloatBuffer(colors.length)
    colorsBuffer.put(colors)
    colorsBuffer.flip()

    // OpenGL expects to draw vertices in counter clockwise order by default
    var indices: Array[Byte] = Array(
      0, 1, 2,
      2, 3, 0
    )
    indicesCount = indices.length
    var indicesBuffer: ByteBuffer = BufferUtils.createByteBuffer(indicesCount)
    indicesBuffer.put(indices)
    indicesBuffer.flip()

    // Create a new Vertex Array Object in memory and select it (bind)
    vaoId = GL30.glGenVertexArrays()
    GL30.glBindVertexArray(vaoId)

    // Create a new Vertex Buffer Object in memory and select it (bind) - VERTICES
    vboId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vboId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, verticesBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(0, 4, GL11.GL_FLOAT, false, 0, 0)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    // Create a new VBO for the indices and select it (bind) - COLORS
    vbocId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, vbocId)
    GL15.glBufferData(GL15.GL_ARRAY_BUFFER, colorsBuffer, GL15.GL_STATIC_DRAW)
    GL20.glVertexAttribPointer(1, 4, GL11.GL_FLOAT, false, 0, 0)
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)

    // Deselect (bind to 0) the VAO
    GL30.glBindVertexArray(0)

    // Create a new VBO for the indices and select it (bind) - INDICES
    vboiId = GL15.glGenBuffers()
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)
    GL15.glBufferData(GL15.GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL15.GL_STATIC_DRAW)
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    println("Setup Quad Finished")
  }

  def setupShaders() = {
    var errorCheckValue: Int = GL11.glGetError()

    // Load the vertex shader
    vsId = this.loadShader(vertexShader, GL20.GL_VERTEX_SHADER)
    // Load the fragment shader
    fsId = this.loadShader(fragmentShader, GL20.GL_FRAGMENT_SHADER)

    // Create a new shader program that links both shaders
    pId = GL20.glCreateProgram()
    GL20.glAttachShader(pId, vsId)
    GL20.glAttachShader(pId, fsId)

    // Position information will be attribute 0
    GL20.glBindAttribLocation(pId, 0, "in_Position")
    // Color information will be attribute 1
    GL20.glBindAttribLocation(pId, 1, "in_Color")

    GL20.glLinkProgram(pId)
    GL20.glValidateProgram(pId)

    errorCheckValue = GL11.glGetError()
    if (errorCheckValue != GL11.GL_NO_ERROR) {
      System.out.println("ERROR - Could not create the shaders:")
      System.exit(-1)
    }

    println("Setup shaders finished")
  }

  def loopCycle() = {
    GL11.glClear(GL11.GL_COLOR_BUFFER_BIT)

    GL20.glUseProgram(pId)

    // Bind to the VAO that has all the information about the vertices
    GL30.glBindVertexArray(vaoId)
    GL20.glEnableVertexAttribArray(0)
    GL20.glEnableVertexAttribArray(1)

    // Bind to the index VBO that has all the information about the order of the vertices
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, vboiId)

    // Draw the vertices
    GL11.glDrawElements(GL11.GL_TRIANGLES, indicesCount, GL11.GL_UNSIGNED_BYTE, 0)

    // Put everything back to default (deselect)
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    GL20.glDisableVertexAttribArray(0)
    GL20.glDisableVertexAttribArray(1)
    GL30.glBindVertexArray(0)
    GL20.glUseProgram(0)
  }

  def destroyOpenGL() = {
    // Delete the shaders
    GL20.glUseProgram(0)
    GL20.glDetachShader(pId, vsId)
    GL20.glDetachShader(pId, fsId)

    GL20.glDeleteShader(vsId)
    GL20.glDeleteShader(fsId)
    GL20.glDeleteProgram(pId)

    // Select the VAO
    GL30.glBindVertexArray(vaoId)

    // Disable the VBO index from the VAO attributes list
    GL20.glDisableVertexAttribArray(0)
    GL20.glDisableVertexAttribArray(1)

    // Delete the vertex VBO
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vboId)

    // Delete the color VBO
    GL15.glBindBuffer(GL15.GL_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vbocId)

    // Delete the index VBO
    GL15.glBindBuffer(GL15.GL_ELEMENT_ARRAY_BUFFER, 0)
    GL15.glDeleteBuffers(vboiId)

    // Delete the VAO
    GL30.glBindVertexArray(0)
    GL30.glDeleteVertexArrays(vaoId)

    GLFW.glfwDestroyWindow(window)
    GLFW.glfwTerminate()
  }

  def loadShader(shaderSource: String, kind: Int): Int = {
    val shaderID = GL20.glCreateShader(kind)
    GL20.glShaderSource(shaderID, shaderSource)
    GL20.glCompileShader(shaderID)

    shaderID
  }
}
