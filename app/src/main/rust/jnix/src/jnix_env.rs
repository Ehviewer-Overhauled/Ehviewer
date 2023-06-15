use jni::{
    objects::{GlobalRef, JObject},
    JNIEnv,
};
use once_cell::sync::Lazy;
use parking_lot::Mutex;
use std::{borrow::Cow, collections::HashMap, ops::Deref};

static CLASS_CACHE: Lazy<Mutex<HashMap<String, GlobalRef>>> =
    Lazy::new(|| Mutex::new(HashMap::new()));

/// Provides access to JNI functions.
///
/// Also provides an internal cache for loaded classes.
pub struct JnixEnv<'env> {
    env: JNIEnv<'env>,
}

impl<'env> From<JNIEnv<'env>> for JnixEnv<'env> {
    fn from(env: JNIEnv<'env>) -> Self {
        JnixEnv { env }
    }
}

impl<'env> Deref for JnixEnv<'env> {
    type Target = JNIEnv<'env>;

    fn deref(&self) -> &Self::Target {
        &self.env
    }
}

impl<'env> JnixEnv<'env> {
    /// Retrieves a global reference to a Class object.
    ///
    /// Tries to retrieve it from the class cache. If it's not present in the cache, it is loaded
    /// into the cache first.
    ///
    /// # Panics
    ///
    /// This method will panic if the class can't be loaded or if a global reference can't be
    /// obtained to the Class object.
    pub fn get_class<'a>(&self, class_name: impl Into<Cow<'a, str>>) -> GlobalRef {
        let class_name = class_name.into();
        let mut cache = CLASS_CACHE.lock();

        if let Some(class) = cache.get(class_name.as_ref()) {
            class.clone()
        } else {
            let class = self.load_class(class_name.as_ref());

            cache.insert(class_name.into_owned(), class.clone());

            class
        }
    }

    /// Loads a class and stores it in the class cache.
    pub fn preload_classes(&self, class_names: impl IntoIterator<Item = impl Into<String>>) {
        let mut cache = CLASS_CACHE.lock();

        for class_name in class_names {
            let class_name = class_name.into();
            let class = self.load_class(&class_name);

            cache.insert(class_name, class);
        }
    }

    fn load_class(&self, class_name: impl AsRef<str>) -> GlobalRef {
        let class_name = class_name.as_ref();
        let local_ref = self
            .env
            .find_class(class_name)
            .expect(&format!("Failed to find {} Java class", class_name));

        self.env.new_global_ref(JObject::from(local_ref)).expect(
            "Failed to convert local reference to Java class object into a global reference",
        )
    }
}
