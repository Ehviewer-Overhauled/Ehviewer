/*
 * Copyright 2018 Hippo Seven
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#include "InArchive.h"

#include <Windows/PropVariant.h>
#include <7zip/ICoder.h>
#include <7zip/IPassword.h>

#include "BlackHole.h"
#include "Log.h"
#include "Utils.h"

using namespace a7zip;

class ArchiveExtractCallback :
    public IArchiveExtractCallback,
    public ICryptoGetTextPassword,
    public CMyUnknownImp
{
 public:
  ArchiveExtractCallback(UInt32 index, BSTR password, CMyComPtr<ISequentialOutStream>& out_stream);
  ~ArchiveExtractCallback();

 public:
  MY_UNKNOWN_IMP2(IArchiveExtractCallback, ICryptoGetTextPassword)

  STDMETHOD(SetTotal)(UInt64 total);
  STDMETHOD(SetCompleted)(const UInt64 *completeValue);

  STDMETHOD(GetStream)(UInt32 index, ISequentialOutStream** outStream, Int32 askExtractMode);
  STDMETHOD(PrepareOperation)(Int32 askExtractMode);
  STDMETHOD(SetOperationResult)(Int32 opRes);

  STDMETHOD(CryptoGetTextPassword)(BSTR *password);

  HRESULT GetBetterResult(HRESULT result);

 private:
  UInt32 index;
  BSTR password;
  CMyComPtr<ISequentialOutStream> out_stream;
  bool has_asked_password;
};

ArchiveExtractCallback::ArchiveExtractCallback(
    UInt32 index,
    BSTR password,
    CMyComPtr<ISequentialOutStream>& out_stream
) :
    index(index),
    password(::SysAllocString(password)),
    out_stream(out_stream),
    has_asked_password(false) {}

ArchiveExtractCallback::~ArchiveExtractCallback() {
  ::SysFreeString(password);
}

HRESULT ArchiveExtractCallback::SetTotal(UInt64 total) {
  // Ignored
  return S_OK;
}

HRESULT ArchiveExtractCallback::SetCompleted(const UInt64 *completeValue) {
  // Ignored
  return S_OK;
}

HRESULT ArchiveExtractCallback::GetStream(
    UInt32 index,
    ISequentialOutStream** outStream,
    Int32 askExtractMode
) {
  // If it's not extract mode or the index is different, return a black hole to skip data
  if (askExtractMode != NArchive::NExtract::NAskMode::kExtract || this->index != index) {
    CMyComPtr<ISequentialOutStream> black_hole(new BlackHole());
    *outStream = black_hole.Detach();
    return S_OK;
  }

  if (out_stream == nullptr) {
    return E_NO_OUT_STREAM;
  }

  CMyComPtr<ISequentialOutStream> steam_copy(out_stream);
  *outStream = steam_copy.Detach();

  return S_OK;
}

HRESULT ArchiveExtractCallback::PrepareOperation(Int32 askExtractMode) {
  // Always return S_OK
  return S_OK;
}

HRESULT ArchiveExtractCallback::SetOperationResult(Int32 opRes) {
  // Stop extracting action if operation result is not OK
  switch (opRes) {
    case NArchive::NExtract::NOperationResult::kOK:
      return S_OK;
    case NArchive::NExtract::NOperationResult::kUnsupportedMethod:
      return E_UNSUPPORTED_METHOD;
    case NArchive::NExtract::NOperationResult::kDataError:
      return E_DATA_ERROR;
    case NArchive::NExtract::NOperationResult::kCRCError:
      return E_CRC_ERROR;
    case NArchive::NExtract::NOperationResult::kUnavailable:
      return E_UNAVAILABLE;
    case NArchive::NExtract::NOperationResult::kUnexpectedEnd:
      return E_UNEXPECTED_END;
    case NArchive::NExtract::NOperationResult::kDataAfterEnd:
      return E_DATA_AFTER_END;
    case NArchive::NExtract::NOperationResult::kIsNotArc:
      return E_IS_NOT_ARC;
    case NArchive::NExtract::NOperationResult::kHeadersError:
      return E_HEADERS_ERROR;
    case NArchive::NExtract::NOperationResult::kWrongPassword:
      return E_WRONG_PASSWORD;
    default:
      return E_UNKNOWN_ERROR;
  }
}

HRESULT ArchiveExtractCallback::CryptoGetTextPassword(BSTR* password) {
  has_asked_password = true;
  *password = ::SysAllocString(this->password);
  return this->password != nullptr ? S_OK : E_NO_PASSWORD;
}

HRESULT ArchiveExtractCallback::GetBetterResult(HRESULT result) {
  if (result == S_OK) {
    return S_OK;
  } else if (has_asked_password) {
    if (password != nullptr) {
      return E_WRONG_PASSWORD;
    } else {
      return E_NO_PASSWORD;
    }
  } else {
    return result;
  }
}

InArchive::InArchive(
    InArchive* parent,
    CMyComPtr<IInArchive>& in_archive,
    AString& format_name
) :
    parent(parent),
    in_archive(in_archive),
    format_name(format_name) { }

InArchive::~InArchive() {
  this->in_archive->Close();
  if (parent != nullptr) {
    delete parent;
    parent = nullptr;
  }
}

const AString& InArchive::GetFormatName() {
  return this->format_name;
}

HRESULT InArchive::GetNumberOfEntries(UInt32& number) {
  return this->in_archive->GetNumberOfItems(&number);
}

static PropType VarTypeToPropType(VARTYPE var_enum) {
  // TODO VT_ERROR
  switch (var_enum) {
    case VT_EMPTY:
      return PT_EMPTY;
    case VT_BOOL:
      return PT_BOOL;
    case VT_I1:
    case VT_I2:
    case VT_I4:
    case VT_INT:
    case VT_UI1:
    case VT_UI2:
    case VT_UI4:
    case VT_UINT:
      return PT_INT;
    case VT_I8:
    case VT_UI8:
    case VT_FILETIME:
      return PT_LONG;
    case VT_BSTR:
      return PT_STRING;
    default:
      return PT_UNKNOWN;
  }
}

#define GET_ARCHIVE_PROPERTY_START(METHOD_NAME, VALUE_TYPE)                               \
HRESULT InArchive::METHOD_NAME(PROPID prop_id, VALUE_TYPE value) {                        \
  NWindows::NCOM::CPropVariant prop;                                                      \
  RETURN_SAME_IF_NOT_ZERO(this->in_archive->GetArchiveProperty(prop_id, &prop));

#define GET_ARCHIVE_PROPERTY_END                                                          \
}

#define GET_ENTRY_PROPERTY_START(METHOD_NAME, VALUE_TYPE)                                 \
HRESULT InArchive::METHOD_NAME(UInt32 index, PROPID prop_id, VALUE_TYPE value) {          \
  NWindows::NCOM::CPropVariant prop;                                                      \
  RETURN_SAME_IF_NOT_ZERO(this->in_archive->GetProperty(index, prop_id, &prop));

#define GET_ENTRY_PROPERTY_END                                                            \
}

#define GET_PROPERTY_TYPE                                                                 \
  *value = VarTypeToPropType(prop.vt);                                                    \
  return S_OK;

GET_ARCHIVE_PROPERTY_START(GetArchivePropertyType, PropType*)
  GET_PROPERTY_TYPE
GET_ARCHIVE_PROPERTY_END

GET_ENTRY_PROPERTY_START(GetEntryPropertyType, PropType*)
  GET_PROPERTY_TYPE
GET_ENTRY_PROPERTY_END

#define GET_BOOLEAN_PROPERTY                                                              \
  switch (prop.vt) {                                                                      \
    case VT_BOOL:                                                                         \
      *value = prop.boolVal != 0;                                                         \
      return S_OK;                                                                        \
    case VT_EMPTY:                                                                        \
      return E_EMPTY_PROP;                                                                \
    default:                                                                              \
      return E_INCONSISTENT_PROP_TYPE;                                                    \
  }

GET_ARCHIVE_PROPERTY_START(GetArchiveBooleanProperty, bool*)
  GET_BOOLEAN_PROPERTY
GET_ARCHIVE_PROPERTY_END

GET_ENTRY_PROPERTY_START(GetEntryBooleanProperty, bool*)
  GET_BOOLEAN_PROPERTY
GET_ENTRY_PROPERTY_END

#define GET_INT_PROPERTY                                                                  \
  switch (prop.vt) {                                                                      \
    case VT_I1:                                                                           \
      *value = prop.cVal;                                                                 \
      return S_OK;                                                                        \
    case VT_I2:                                                                           \
      *value = prop.iVal;                                                                 \
      return S_OK;                                                                        \
    case VT_I4:                                                                           \
      *value = prop.lVal;                                                                 \
      return S_OK;                                                                        \
    case VT_INT:                                                                          \
      *value = prop.intVal;                                                               \
      return S_OK;                                                                        \
    case VT_UI1:                                                                          \
      *value = prop.bVal;                                                                 \
      return S_OK;                                                                        \
    case VT_UI2:                                                                          \
      *value = prop.uiVal;                                                                \
      return S_OK;                                                                        \
    case VT_UI4:                                                                          \
      *value = prop.ulVal;                                                                \
      return S_OK;                                                                        \
    case VT_UINT:                                                                         \
      *value = prop.uintVal;                                                              \
      return S_OK;                                                                        \
    case VT_EMPTY:                                                                        \
      return E_EMPTY_PROP;                                                                \
    default:                                                                              \
      return E_INCONSISTENT_PROP_TYPE;                                                    \
  }

GET_ARCHIVE_PROPERTY_START(GetArchiveIntProperty, Int32*)
  GET_INT_PROPERTY
GET_ARCHIVE_PROPERTY_END

GET_ENTRY_PROPERTY_START(GetEntryIntProperty, Int32*)
  GET_INT_PROPERTY
GET_ENTRY_PROPERTY_END

static const UInt64 FILE_TIME_OFFSET = (369 * 365 + 89) * 86400ULL * 10000000ULL;
static const UInt64 FILE_TIME_MULTIPLE = 10000ULL;

#define GET_LONG_PROPERTY                                                                 \
  switch (prop.vt) {                                                                      \
    case VT_I8:                                                                           \
      *value = prop.hVal.QuadPart;                                                        \
      return S_OK;                                                                        \
    case VT_UI8:                                                                          \
      *value = prop.uhVal.QuadPart;                                                       \
      return S_OK;                                                                        \
    case VT_FILETIME:                                                                     \
      *value = (*reinterpret_cast<UInt64*>(&prop.filetime) - FILE_TIME_OFFSET) /          \
                   FILE_TIME_MULTIPLE;                                                    \
      return S_OK;                                                                        \
    case VT_EMPTY:                                                                        \
      return E_EMPTY_PROP;                                                                \
    default:                                                                              \
      return E_INCONSISTENT_PROP_TYPE;                                                    \
  }

GET_ARCHIVE_PROPERTY_START(GetArchiveLongProperty, Int64*)
  GET_LONG_PROPERTY
GET_ARCHIVE_PROPERTY_END

GET_ENTRY_PROPERTY_START(GetEntryLongProperty, Int64*)
  GET_LONG_PROPERTY
GET_ENTRY_PROPERTY_END

#define GET_STRING_PROPERTY                                                               \
  switch (prop.vt) {                                                                      \
    case VT_BSTR:                                                                         \
      *value = ::SysAllocString(prop.bstrVal);                                            \
      return S_OK;                                                                        \
    case VT_EMPTY:                                                                        \
      return E_EMPTY_PROP;                                                                \
    default:                                                                              \
      return E_INCONSISTENT_PROP_TYPE;                                                    \
  }

GET_ARCHIVE_PROPERTY_START(GetArchiveStringProperty, BSTR*)
  GET_STRING_PROPERTY
GET_ARCHIVE_PROPERTY_END

GET_ENTRY_PROPERTY_START(GetEntryStringProperty, BSTR*)
  GET_STRING_PROPERTY
GET_ENTRY_PROPERTY_END

HRESULT InArchive::GetEntryStream(UInt32 index, ISequentialInStream** stream) {
  *stream = nullptr;
  CMyComPtr<IInArchiveGetStream> in_archive_get_stream;
  in_archive->QueryInterface(IID_IInArchiveGetStream, reinterpret_cast<void **>(&in_archive_get_stream));
  if (in_archive_get_stream != nullptr) {
    return in_archive_get_stream->GetStream(index, stream);
  } else {
    return E_NOTIMPL;
  }
}

HRESULT InArchive::ExtractEntry(UInt32 index, BSTR password, CMyComPtr<ISequentialOutStream>& out_stream) {
  CMyComPtr<ArchiveExtractCallback> callback(new ArchiveExtractCallback(index, password, out_stream));
  HRESULT result = this->in_archive->Extract(&index, 1, false, callback);
  return callback->GetBetterResult(result);
}
