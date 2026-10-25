# A headless Minecraft client: Xvfb for the display, Mesa llvmpipe for OpenGL, no audio device.
FROM eclipse-temurin:17-jdk

RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        xvfb \
        libgl1 libglx-mesa0 libgl1-mesa-dri \
        libx11-6 libxext6 libxrandr2 libxinerama1 libxcursor1 libxi6 libxxf86vm1 \
        rsync curl \
    && rm -rf /var/lib/apt/lists/*

ENV LIBGL_ALWAYS_SOFTWARE=1 \
    GALLIUM_DRIVER=llvmpipe \
    ALSOFT_DRIVERS=null \
    GRADLE_USER_HOME=/gradle

WORKDIR /work
