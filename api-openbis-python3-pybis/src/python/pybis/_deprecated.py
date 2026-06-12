#
#   Copyright ETH 2026 Zürich, Scientific IT Services
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
"""Backport of :func:`warnings.deprecated` (PEP 702, Python 3.13+).

On older interpreters provides a fallback that works on classes (wrapping
``__init__``) and plain functions alike.
"""

from __future__ import annotations

import sys

if sys.version_info >= (3, 13):
    from warnings import deprecated
else:
    import functools
    from typing import Any, Callable
    from warnings import warn

    def deprecated(msg: str) -> Callable[[Any], Any]:
        """Mark a class or function as deprecated.

        Args:
            msg: The deprecation message shown in the warning.

        Returns:
            A decorator emitting a ``DeprecationWarning`` on use.
        """

        def decorator(target: Any) -> Any:
            if isinstance(target, type):
                orig_init = target.__init__  # type: ignore[misc]  # reason: decorating a class object, not an instance

                @functools.wraps(orig_init)
                def __init__(self: Any, *args: Any, **kwargs: Any) -> None:
                    warn(msg, DeprecationWarning, stacklevel=2)
                    orig_init(self, *args, **kwargs)

                target.__init__ = __init__  # type: ignore[misc]  # reason: decorating a class object, not an instance
                return target

            @functools.wraps(target)
            def wrapper(*args: Any, **kwargs: Any) -> Any:
                warn(msg, DeprecationWarning, stacklevel=2)
                return target(*args, **kwargs)

            return wrapper

        return decorator


__all__ = ["deprecated"]
