/*
   Callback implementations.

   Copyright 2010 Glencoe Software, Inc. All rights reserved.
   Use is subject to license terms supplied in LICENSE.txt

*/

#include <omero/ClientErrors.h>
#include <omero/callbacks.h>
#include <omero/RTypesI.h>
#include <IceUtil/Time.h>
#include <IceUtil/UUID.h>

using namespace std;
using namespace IceUtil;
using namespace omero;
using namespace omero::grid;
using namespace omero::rtypes;
using namespace omero::api::_cpp_delete;

namespace omero {

    namespace callbacks {


        //
        // ProcessCallback
        //

        ProcessCallbackI::ProcessCallbackI(
                const Ice::ObjectAdapterPtr& adapter,
                const ProcessPrx& process,
                bool poll) :
            ProcessCallback(),
            adapter(adapter),
            id(Ice::Identity()),
            poll(poll),
            result(std::string()),
            process(process) {

            std::string uuid = generateUUID();
            this->id.category = "ProcessCallback";
            this->id.name = uuid;
            Ice::ObjectPrx prx = adapter->add(this, this->id);
            omero::grid::ProcessCallbackPrx pcb = ProcessCallbackPrx::uncheckedCast(prx);
            process->registerCallback(pcb);

        };

	ProcessCallbackI::~ProcessCallbackI() {
            this->adapter->remove(this->id);
        };

        std::string ProcessCallbackI::block(long ms) {
            if (poll) {
                try {
                    RIntPtr rc = process->poll();
                    if (rc) {
                        processFinished(rc->getValue());
                    }
                } catch (const Ice::Exception& ex) {
                    cerr << "Error calling poll: " << ex << endl;
                }
            }
            event.wait(Time::milliSeconds(ms));
            return result; // Possibly empty
        };

        void ProcessCallbackI::processCancelled(bool success, const Ice::Current& current) {
            result = CANCELLED;
            event.set();
        };

        void ProcessCallbackI::processFinished(int returncode, const Ice::Current& current) {
            result = FINISHED;
            event.set();
        };

        void ProcessCallbackI::processKilled(bool success, const Ice::Current& current) {
            result = KILLED;
            event.set();
        };

        //
        // DeleteCallback
        //

        DeleteCallbackI::DeleteCallbackI(
            const Ice::ObjectAdapterPtr& adapter, const OME_API_DEL::DeleteHandlePrx handle) :
            adapter(adapter),
            poll(true),
            handle(handle) {
        };

	DeleteCallbackI::~DeleteCallbackI() {
            handle->close();
        };

        DeleteReports DeleteCallbackI::loop(int loops, long ms) {
            int count = 0;
            RIntPtr errors;
            while (!errors && count < loops) {
                errors = block(ms);
                count++;
            }

            if (!errors) {
                int waited = (ms/1000) * loops;
                stringstream ss;
                ss << "Delete unfinished after " << waited << "seconds.";
                throw LockTimeout("", "", ss.str(), 5000L, waited);
            } else {
                return handle->report();
            }
        };

        RIntPtr DeleteCallbackI::block(long ms) {
            if (poll) {
                try {
                    if (handle->finished()) {
                            try {
                                finished(handle->errors());
                            } catch (const Ice::Exception& ex) {
                                cerr << "Error calling DeleteCallbackI.finished: " << ex << endl;
                            }
                    }
                } catch (const Ice::ObjectNotExistException& onee) {
                    throw omero::ClientError(__FILE__, __LINE__, "Handle is gone!");
                } catch (const Ice::Exception& ex) {
                    cerr << "Error polling DeleteHandle:" << ex << endl;
                }
            }

            event.wait(Time::milliSeconds(ms));
            return result; // Possibly empty

        };

        void DeleteCallbackI::finished(int errors) {
            result = rint(errors);
            event.set();
        };

        //
        // CmdCallback
        //

        CmdCallbackI::CmdCallbackI(
            const Ice::ObjectAdapterPtr& adapter, const omero::cmd::HandlePrx handle, std::string category, bool closeHandle) :
            omero::cmd::CmdCallback(),
            adapter(adapter),
            handle(handle),
            closeHandle(closeHandle) {
                doinit(category);
        };

        CmdCallbackI::CmdCallbackI(
            const omero::client_ptr& client, const omero::cmd::HandlePrx handle, bool closeHandle) :
            omero::cmd::CmdCallback(),
            adapter(client->getObjectAdapter()),
            handle(handle),
            closeHandle(closeHandle) {
                doinit(client->getCategory());
        };

        void CmdCallbackI::doinit(std::string category) {
            this->id = Ice::Identity();
            this->id.name = IceUtil::generateUUID();
            this->id.category = category;
            const omero::cmd::CmdCallbackPtr ptr(this);
            Ice::ObjectPrx prx = adapter->add(ptr, id);
            omero::cmd::CmdCallbackPrx cb = omero::cmd::CmdCallbackPrx::uncheckedCast(prx);
            handle->addCallback(cb);
            // Now check just in case the process exited VERY quickly
            poll();
        };

	CmdCallbackI::~CmdCallbackI() {
            adapter->remove(id); // OK ADAPTER USAGE
            if (closeHandle) {
                handle->close();
            }
        };

        omero::cmd::ResponsePtr CmdCallbackI::getResponse() {
            IceUtil::RecMutex::Lock lock(mutex);
            return state.first;
        };

        omero::cmd::StatusPtr CmdCallbackI::getStatus() {
            IceUtil::RecMutex::Lock lock(mutex);
            return state.second;
        };

        omero::cmd::StatusPtr CmdCallbackI::getStatusOrThrow() {
            IceUtil::RecMutex::Lock lock(mutex);
            omero::cmd::StatusPtr s = state.second;
            if (!s) {
                throw ClientError(__FILE__, __LINE__, "Status not present!");
            }
            return s;
        };

        int count(omero::cmd::StateList list, omero::cmd::State s) {
            int c = 0;
            omero::cmd::StateList::iterator it;
            for (it=list.begin(); it != list.end(); it++) {
                if (*it == s) {
                    c++;
                }
            }
            return c;
        }

        bool CmdCallbackI::isCancelled() {
            omero::cmd::StatusPtr s = getStatusOrThrow();
            return count(s->flags, omero::cmd::CANCELLED) > 0;

        };

        bool CmdCallbackI::isFailure() {
            omero::cmd::StatusPtr s = getStatusOrThrow();
            return count(s->flags, omero::cmd::FAILURE) > 0;

        };

        omero::cmd::ResponsePtr CmdCallbackI::loop(int loops, long ms) {
            int count = 0;
            bool found = false;
            while (count < loops) {
                count++;
                found = block(ms);
                if (found) {
                    break;
                }
            }

            if (found) {
                return getResponse();
            } else {
                int waited = (ms/1000) * loops;
                stringstream ss;
                ss << "Cmd unfinished after " << waited << "seconds.";
                throw LockTimeout("", "", ss.str(), 5000L, waited);
            }
        };

        bool CmdCallbackI::block(long ms) {
            return event.wait(Time::milliSeconds(ms));
        };

        void CmdCallbackI::poll() {
            omero::cmd::ResponsePtr rsp = handle->getResponse();
            if (rsp) {
                omero::cmd::StatusPtr s = handle->getStatus();
                finished(rsp, s, Ice::Current()); // Only time that current should be null.
            }
        };

        void CmdCallbackI::step(int complete, int total, const Ice::Current& current) {
            // no-op
        };

        void CmdCallbackI::finished(const omero::cmd::ResponsePtr& rsp,
                const omero::cmd::StatusPtr& status, const Ice::Current& current) {
            std::pair<omero::cmd::ResponsePtr, omero::cmd::StatusPtr> s(rsp, status);
            this->state = s;
            event.set();
            onFinished(rsp, status, current);
        };

        void CmdCallbackI::onFinished(const omero::cmd::ResponsePtr& rsp,
                const omero::cmd::StatusPtr& status, const Ice::Current& current) {
            // no-op
        };
    };

};
