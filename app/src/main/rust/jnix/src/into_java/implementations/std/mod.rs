mod net;

use crate::{IntoJava, JnixEnv};
use jni::{
    objects::{AutoLocal, JList, JObject, JValue},
    sys::{jboolean, jdouble, jint, jlong, jshort, jsize, JNI_FALSE, JNI_TRUE},
};

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for bool {
    const JNI_SIGNATURE: &'static str = "Z";

    type JavaType = jboolean;

    fn into_java(self, _: &'borrow JnixEnv<'env>) -> Self::JavaType {
        if self {
            JNI_TRUE
        } else {
            JNI_FALSE
        }
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for i16 {
    const JNI_SIGNATURE: &'static str = "S";

    type JavaType = jshort;

    fn into_java(self, _: &'borrow JnixEnv<'env>) -> Self::JavaType {
        self as jshort
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for i32 {
    const JNI_SIGNATURE: &'static str = "I";

    type JavaType = jint;

    fn into_java(self, _: &'borrow JnixEnv<'env>) -> Self::JavaType {
        self as jint
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for i64 {
    const JNI_SIGNATURE: &'static str = "J";

    type JavaType = jlong;

    fn into_java(self, _: &'borrow JnixEnv<'env>) -> Self::JavaType {
        self as jlong
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for f64 {
    const JNI_SIGNATURE: &'static str = "D";

    type JavaType = jdouble;

    fn into_java(self, _: &'borrow JnixEnv<'env>) -> Self::JavaType {
        self as jdouble
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for &'_ [u8] {
    const JNI_SIGNATURE: &'static str = "[B";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        let size = self.len();
        let array = env
            .new_byte_array(size as jsize)
            .expect("Failed to create a Java array of bytes");

        let data = unsafe { std::slice::from_raw_parts(self.as_ptr() as *const i8, size) };

        env.set_byte_array_region(array, 0, data)
            .expect("Failed to copy bytes to Java array");

        env.auto_local(JObject::from(array))
    }
}

macro_rules! impl_into_java_for_array {
    ($element_type:ty) => {
        impl_into_java_for_array!(
            $element_type,
             0  1  2  3  4  5  6  7
             8  9 10 11 12 13 14 15
            16 17 18 19 20 21 22 23
            24 25 26 27 28 29 30 31
            32
        );
    };

    ($element_type:ty, $( $count:tt )*) => {
        $(
            impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for [$element_type; $count] {
                const JNI_SIGNATURE: &'static str = "[B";

                type JavaType = AutoLocal<'env, 'borrow>;

                fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
                    (&self as &[$element_type]).into_java(env)
                }
            }
        )*
    };
}

impl_into_java_for_array!(u8);

impl<'borrow, 'env, T> IntoJava<'borrow, 'env> for Option<T>
where
    'env: 'borrow,
    T: IntoJava<'borrow, 'env, JavaType = AutoLocal<'env, 'borrow>>,
{
    const JNI_SIGNATURE: &'static str = T::JNI_SIGNATURE;

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        match self {
            Some(t) => t.into_java(env),
            None => env.auto_local(JObject::null()),
        }
    }
}

impl<'borrow, 'env> IntoJava<'borrow, 'env> for Option<bool>
where
    'env: 'borrow,
{
    const JNI_SIGNATURE: &'static str = "Ljava/lang/Boolean;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        match self {
            Some(value) => {
                let class = env.get_class("java/lang/Boolean");
                let boxed_boolean = env
                    .new_object(&class, "(Z)V", &[JValue::Bool(value as jboolean)])
                    .expect("Failed to create boxed Boolean object");

                env.auto_local(boxed_boolean)
            }
            None => env.auto_local(JObject::null()),
        }
    }
}

impl<'borrow, 'env> IntoJava<'borrow, 'env> for Option<i32>
where
    'env: 'borrow,
{
    const JNI_SIGNATURE: &'static str = "Ljava/lang/Integer;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        match self {
            Some(value) => {
                let class = env.get_class("java/lang/Integer");
                let boxed_boolean = env
                    .new_object(&class, "(I)V", &[JValue::Int(value as jint)])
                    .expect("Failed to create boxed Integer object");

                env.auto_local(boxed_boolean)
            }
            None => env.auto_local(JObject::null()),
        }
    }
}

impl<'borrow, 'env, T> IntoJava<'borrow, 'env> for Vec<T>
where
    'env: 'borrow,
    T: IntoJava<'borrow, 'env, JavaType = AutoLocal<'env, 'borrow>>,
{
    const JNI_SIGNATURE: &'static str = "Ljava/util/ArrayList;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        let initial_capacity = self.len();
        let parameters = [JValue::Int(initial_capacity as jint)];

        let class = env.get_class("java/util/ArrayList");
        let list_object = env
            .new_object(&class, "(I)V", &parameters)
            .expect("Failed to create ArrayList object");

        let list =
            JList::from_env(env, list_object).expect("Failed to create JList from ArrayList");

        for element in self {
            list.add(element.into_java(env).as_obj())
                .expect("Failed to add element to ArrayList");
        }

        env.auto_local(list_object)
    }
}

impl<'borrow, 'env: 'borrow> IntoJava<'borrow, 'env> for String {
    const JNI_SIGNATURE: &'static str = "Ljava/lang/String;";

    type JavaType = AutoLocal<'env, 'borrow>;

    fn into_java(self, env: &'borrow JnixEnv<'env>) -> Self::JavaType {
        let jstring = env.new_string(&self).expect("Failed to create Java String");

        env.auto_local(jstring)
    }
}
