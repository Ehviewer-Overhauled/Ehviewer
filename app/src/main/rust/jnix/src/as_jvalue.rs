use jni::objects::{AutoLocal, JValue};

/// Returns a value as its [`JValue`] representation.
///
/// [`JValue`]: jni::objects::JValue
pub trait AsJValue<'env> {
    /// Returns the [`JValue`] representation of the type.
    ///
    /// [`JValue`]: jni::objects::JValue
    fn as_jvalue<'borrow>(&'borrow self) -> JValue<'borrow>
    where
        'env: 'borrow;
}

impl<'env_borrow, 'env: 'env_borrow> AsJValue<'env> for AutoLocal<'env, 'env_borrow> {
    fn as_jvalue<'borrow>(&'borrow self) -> JValue<'borrow>
    where
        'env: 'borrow,
    {
        JValue::Object(self.as_obj())
    }
}

macro_rules! impl_for_primitives {
    ( $( $primitive:ty ),* $(,)* ) => {
        $(
            impl<'env> AsJValue<'env> for $primitive {
                fn as_jvalue<'borrow>(&'borrow self) -> JValue<'borrow>
                where
                    'env: 'borrow,
                {
                    JValue::from(*self)
                }
            }
        )*
    };
}

impl_for_primitives!((), bool, u8, i8, u16, i16, i32, i64, f32, f64);
