$env:JAVA_HOME = 'C:\Users\subbu\.version-fox\sdks\java'
$env:PATH = "C:\Users\subbu\.version-fox\sdks\java\bin;$env:PATH"

Write-Host "Java version:"
& java -version

Write-Host "`nBuilding application..."
& mvn clean package -DskipTests
