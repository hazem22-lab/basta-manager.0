val envExample = file("$rootDir/.env.example")
if (!envExample.exists()) {
    envExample.createNewFile()
}
