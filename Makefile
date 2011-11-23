CP=./glfiles:./glfiles/natives:./processing/opengl:./core.jar:./oscP5.jar:.

camusic:
	javac -cp $(CP) *.java

run:
	java -Djava.library.path=$(CP) CAMusicWrapper

clean:
	@rm -f *.class
	@rm -f *~
