#include <jni.h>
#include <NeuralNetworks.h>
#include <stdint.h>
#include <stdio.h>

static int check(JNIEnv *env, int status, const char *operation) {
    if (status == ANEURALNETWORKS_NO_ERROR) return 1;
    char message[192];
    snprintf(message, sizeof(message), "%s failed, NNAPI status=%d", operation, status);
    jclass exception = (*env)->FindClass(env, "java/lang/IllegalStateException");
    if (exception != NULL) (*env)->ThrowNew(env, exception, message);
    return 0;
}

JNIEXPORT jobjectArray JNICALL
Java_io_zoompilot_jetlink_server_NnapiInventory_nativeInventory(JNIEnv *env, jobject self) {
    (void)self;
    uint32_t count = 0;
    if (!check(env, ANeuralNetworks_getDeviceCount(&count), "getDeviceCount")) return NULL;
    if (count > 1024) {
        check(env, ANEURALNETWORKS_BAD_DATA, "invalid device count");
        return NULL;
    }
    jclass strings = (*env)->FindClass(env, "java/lang/String");
    if (strings == NULL) return NULL;
    jobjectArray result = (*env)->NewObjectArray(env, (jsize)(count * 4), strings, NULL);
    if (result == NULL) return NULL;
    for (uint32_t i = 0; i < count; ++i) {
        ANeuralNetworksDevice *device = NULL;
        const char *name = NULL, *version = NULL;
        int32_t type = 0;
        int64_t feature = 0;
        if (!check(env, ANeuralNetworks_getDevice(i, &device), "getDevice") ||
            !check(env, ANeuralNetworksDevice_getName(device, &name), "getName") ||
            !check(env, ANeuralNetworksDevice_getType(device, &type), "getType") ||
            !check(env, ANeuralNetworksDevice_getVersion(device, &version), "getVersion") ||
            !check(env, ANeuralNetworksDevice_getFeatureLevel(device, &feature), "getFeatureLevel")) return NULL;
        char type_text[32], feature_text[32];
        snprintf(type_text, sizeof(type_text), "%d", type);
        snprintf(feature_text, sizeof(feature_text), "%lld", (long long)feature);
        const char *fields[] = {name, type_text, version, feature_text};
        for (uint32_t j = 0; j < 4; ++j) {
            if (fields[j] == NULL) {
                check(env, ANEURALNETWORKS_UNEXPECTED_NULL, "device metadata");
                return NULL;
            }
            jstring value = (*env)->NewStringUTF(env, fields[j]);
            if (value == NULL) return NULL;
            (*env)->SetObjectArrayElement(env, result, (jsize)(i * 4 + j), value);
            (*env)->DeleteLocalRef(env, value);
            if ((*env)->ExceptionCheck(env)) return NULL;
        }
    }
    return result;
}
