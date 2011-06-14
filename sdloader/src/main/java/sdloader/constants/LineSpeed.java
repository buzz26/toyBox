/*
 * Copyright 2005-2010 the original author or authors.
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sdloader.constants;

/**
 * 回線速度定義用定数.
 * 
 * @author c9katayama
 * 
 */
public interface LineSpeed {

	public static final int NO_LIMIT = -1;
	public static final int ISDN_64K_BPS = 64 * 1000;
	public static final int ISDN_128K_BPS = 128 * 1000;
	public static final int ISDN_256K_BPS = 256 * 1000;
	public static final int ISDN_512K_BPS = 512 * 1000;
	public static final int ADSL_1M_BPS = 1000 * 1000;
	public static final int ADSL_4M_BPS = 4000 * 1000;
	public static final int ADSL_8M_BPS = 8000 * 1000;
	public static final int LAN_10M_BPS = 10000 * 1000;
	public static final int ADSL_20M_BPS = 20000 * 1000;
	public static final int ADSL_40M_BPS = 40000 * 1000;
}
