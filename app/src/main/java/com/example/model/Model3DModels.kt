package com.example.model

enum class Model3DSource(val label: String) {
    PRIMITIVE("3D Primitive"),
    IMPORTED_FILE("Imported Mesh"),
    EXTRUDED_TEXT("3D Extruded Text"),
    IMAGE_PLANE("3D Image Plane")
}

enum class Primitive3DShape(val label: String) {
    CUBE("Cube"),
    SPHERE("Sphere"),
    PYRAMID("Pyramid"),
    TORUS("Torus"),
    CYLINDER("Cylinder"),
    OCTAHEDRON("Diamond Prism"),
    PHONE_SLAB("3D Phone")
}

enum class RenderMode3D(val label: String) {
    SHADED("Shaded"),
    SOLID("Flat Solid"),
    WIREFRAME("Wireframe")
}

data class Vec3(
    val x: Float,
    val y: Float,
    val z: Float
)

data class Face3D(
    val i0: Int,
    val i1: Int,
    val i2: Int,
    val tintFactor: Float = 1f
)

data class Mesh3DData(
    val vertices: List<Vec3>,
    val faces: List<Face3D>,
    val name: String
)

data class Model3DConfig(
    val source: Model3DSource = Model3DSource.PRIMITIVE,
    val primitiveShape: Primitive3DShape = Primitive3DShape.CUBE,
    val importedUri: String? = null,
    val importedFileName: String? = null,
    val extrudedText: String = "MOTION 3D",
    val imageUri: String? = null,
    val rotationX: Float = 18f,
    val rotationY: Float = -28f,
    val rotationZ: Float = 0f,
    val depthZ: Float = 0f,
    val extrusionDepth: Float = 0.40f,
    val modelScale: Float = 1.0f,
    val fovDegrees: Float = 45f,
    val materialColorHex: Long = 0xFF00E5FF,
    val metallic: Float = 0.35f,
    val lightAzimuthDeg: Float = 40f,
    val lightElevationDeg: Float = 35f,
    val renderMode: RenderMode3D = RenderMode3D.SHADED,
    val showEdges: Boolean = true
)
