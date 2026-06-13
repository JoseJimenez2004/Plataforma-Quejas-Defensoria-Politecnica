# Definir la ruta base
BASE_PATH="src/main/java/ipn/escom/defensoria/multimedia"

# Crear todos los directorios de una vez
mkdir -p $BASE_PATH/{config,controller,dto,entity,enums,repository,service,utils}

# Crear los archivos correspondientes
touch $BASE_PATH/config/SecurityConfig.java
touch $BASE_PATH/controller/CompressionController.java
touch $BASE_PATH/dto/{CompressionResponseDTO.java,FileMetadataDTO.java}
touch $BASE_PATH/entity/CompressedFile.java
touch $BASE_PATH/enums/FileType.java
touch $BASE_PATH/repository/CompressedFileRepository.java
touch $BASE_PATH/service/{CompressionService.java,ImageCompressionService.java,VideoCompressionService.java,AudioCompressionService.java,PdfCompressionService.java}
touch $BASE_PATH/utils/{FileUtils.java,FFmpegUtils.java}
