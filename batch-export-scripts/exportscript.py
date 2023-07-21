import bpy
import os

dir = "C:\\Users\\jelle\\Desktop\\School\\Projects\\OSRS\\OSRS-Environment-Exporter\\output"

v = [x.path for x in os.scandir(dir) if os.path.isdir(x)]

startPos = 0

for (idx, item) in enumerate(v[startPos:]):
    src=f"{item}\\scene.gltf"

    displayIdx = f"{idx + startPos + 1}/{len(v)}"

    dst=f"{item}\\..\\out{idx + startPos}.fbx"

    print(f"[PROGRESS] Pre-flushing {displayIdx}")
    bpy.ops.wm.read_homefile()
    for obj in bpy.data.objects:
        bpy.data.objects.remove(obj)

    print(f"[PROGRESS] Importing {displayIdx}")
    bpy.ops.import_scene.gltf(filepath=src)
    bpy.ops.object.select_all(action='SELECT')

    print(f"[PROGRESS] Exporting {displayIdx}")
    bpy.ops.export_scene.fbx(filepath=dst, path_mode='COPY', embed_textures=True)
    print("----------------------------------")

print("Done!")
