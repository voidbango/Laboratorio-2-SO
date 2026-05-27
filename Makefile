# Variables
CXX = g++
CXXFLAGS = -Wall -Wextra -std=c++11 -O2

# Nombres de los ejecutables (Ajusta los apellidos según corresponda)
TARGET_ALPHA = LAB2_Torres_Salinas

# Regla por defecto
all: $(TARGET_ALPHA)

# Compilación del Motor Alpha (C++)
$(TARGET_ALPHA): LAB2_Torres_Salinas.cpp
	$(CXX) $(CXXFLAGS) -o $(TARGET_ALPHA) LAB2_Torres_Salinas.cpp

# Limpiar ejecutables y archivos de texto generados
clean:
	rm -f $(TARGET_ALPHA) salida-Fork.txt